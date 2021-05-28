from asyncio.windows_events import PipeServer
from emotemanager import EmoteManager
from os.path import split
from typing import ValuesView
import discord
import asyncio
from discord.ext import tasks, commands
from discord.ext.commands.core import has_permissions
from discord_slash import SlashCommand
from discord_slash.utils.manage_commands import create_option
from emote import Emote
import emoji
import re

import os
import time
import pickle
import random

prefix = "x!"
DISCORD_TOKEN = 'INSERT TOKEN'

bot = commands.Bot(command_prefix=commands.when_mentioned_or(
    prefix), case_insensitive=True)
ems = {}
try:
    with open('data', 'rb') as f:
        prefix, ems = pickle.load(f)
except Exception as e:
    ems = {}
    print("could not retrieve data.\n", e)


@tasks.loop(hours=1)
async def update():
    for guild in bot.guilds:
        em = ems.setdefault(guild.id, EmoteManager(guild))
        await em.update(guild)
    with open('data', 'wb') as f:
        pickle.dump([prefix, ems], f)
    print('Updated data')

update.start()


@bot.event
async def on_ready():
    print('Bot is running')


@bot.event
async def on_message(message):
    if message.author.bot:
        return
    content = message.content.lower()
    server = message.guild
    em = ems.setdefault(server.id, EmoteManager(server))
    if em.eCycle:
        possibleEmotes = content.split(':')
        if len(possibleEmotes) > 1:
            for s in possibleEmotes:
                cnt = 0
                if await em.addEmote(s, server):
                    cnt += 1
                    if cnt >= 2:
                        break

    if em.eReplace != "off":
        webhooks = await message.channel.webhooks()
        if webhooks:
            webhook = webhooks[0]
        else:
            webhook = await message.channel.create_webhook(name="adelaide emotes")
        input = re.split(r':\s*(?![^<>]*\>)', message.content)
        replace = False
        if len(input) > 1:
            for i, s in enumerate(input):
                e = discord.utils.get(server.emojis, name=s)
                if not e and em.eReplace == "all":
                    e = discord.utils.get(bot.emojis, name=s)
                if e:
                    input[i] = str(e)
                    replace = True
        if replace:
            await webhook.send(content=' '.join(input), username=message.author.display_name, avatar_url=str(message.author.avatar_url_as(format='png')))
            await message.delete()

    await bot.process_commands(message)


@bot.command(usage='[+all, emotes]')
@has_permissions(manage_emojis=True)
async def preserve(ctx, *, arg):
    em = ems[ctx.guild.id]
    if arg == '+all':
        for e in em.eList.values():
            if e.inServer:
                e.cycleable = False
    else:
        input = arg.replace(':', ' ').split()
        for s in input:
            if s in em.eList:
                em.eList[s].cycleable = False
    await ctx.send('Done.')


@bot.command(usage='[+all, emotes]')
@has_permissions(manage_emojis=True)
async def unpreserve(ctx, *, arg):
    em = ems[ctx.guild.id]
    if arg == '+all':
        for e in em.eList.values():
            e.cycleable = True
    else:
        input = arg.replace(':', ' ').split()
        for s in input:
            if s in em.eList:
                em.eList[s].cycleable = True
    await ctx.send('Done.')


@bot.command(usage='[on|off]')
@has_permissions(manage_emojis=True)
async def eCycle(ctx, arg):
    em = ems[ctx.guild.id]
    if arg == 'on':
        em.eCycle = True
    elif arg == 'off':
        em.eCycle = False
    else:
        return
    await ctx.send(f'Emote cycle {arg}.')


@bot.command(usage='[off|server|all]')
@has_permissions(manage_emojis=True)
async def eReplace(ctx, arg):
    em = ems[ctx.guild.id]
    if arg != 'all' and arg != 'server' and arg != 'off':
        return
    em.eReplace = arg
    await ctx.send(f'Emote replacement set to {arg}.')


@bot.command()
@has_permissions(manage_emojis=True)
async def addAllEmotes(ctx):
    em = ems[ctx.guild.id]
    cnt = acnt = 0
    emax = em.getServerMax(ctx.guild)
    for e in em.eList:
        animated = em.eList[e].animated
        if animated and acnt >= emax or not animated and cnt >= emax:
            continue
        if await em.addEmote(e, ctx.guild):
            if animated:
                acnt += 1
            else:
                cnt += 1
            # somehow discord.py doesn't automatically rate limit very well
            time.sleep(1)
    await ctx.send(f'Added {cnt} emotes')


@bot.command()
@has_permissions(manage_emojis=True)
async def clearServer(ctx):
    em = ems[ctx.guild.id]
    cnt = 0
    for e in ctx.guild.emojis:
        await e.delete()
        cnt += 1
        if e.name in em.eList:
            em.eList[e.name].inServer = False
    await ctx.send(f'Deleted {cnt} emotes')


@bot.command()
@has_permissions(manage_emojis=True)
async def updCache(ctx):
    em = ems[ctx.guild.id]
    await em.update(ctx.guild)
    with open('data', 'wb') as f:
        pickle.dump([prefix, ems], f)
    await ctx.send('Updated Cache.')


@bot.command(usage='[+all, emotes]')
@has_permissions(administrator=True)
async def clearCache(ctx, *, arg):
    em = ems[ctx.guild.id]
    if arg == '+all':
        em.eList = {}
    else:
        input = arg.replace(':', ' ').split()
        for s in input:
            if s in em.eList:
                em.eList.pop(s)
    await ctx.send('Done.')


@bot.command()
@has_permissions(manage_emojis=True)
async def emotes(ctx,*args):
    for arg in args:
        if arg=="+server":
            inServer=True
        elif arg=="+cached":
            inServer=False
        elif arg=="+freq":
            sortFreq=True
        elif arg=="+cycle":
            cycle=True
        elif arg=="+animated":
            animated=True
    em = ems[ctx.guild.id]
    embed = discord.Embed(title=ctx.guild.name+" Emotes")
    embed.add_field(name="Emote Cycle", value=em.eCycle)
    embed.add_field(name="Emote Replacement", value=em.eReplace)
    embed.add_field(name="Emote Count", value=len(em.eList))
    await ctx.send(embed=embed)
    output = "```" + \
        "{:<18}{:<9}{:<6}{:<9}{:<4}\n".format(
            "emote", "inServer", "cycle", "lastOc", "freq")
    for e in em.eList.values():
        output += "{:<25}{:<2}{:<2}{:<12}{:<4}\n".format(
            e.file, e.inServer, e.cycleable, round(e.lastOc), e.freq)
        if len(output) > 1800:
            output += '```'
            await ctx.send(output)
            output = "```"
    if len(output):
        output += '```'
        await ctx.send(output)


@bot.command()
async def beep(ctx):
    await ctx.send('Boop.')


@bot.command()
async def coinflip(ctx):
    coin = random.randint(False, True)
    result = "The coin landed on "+("heads" if coin else "tails")
    embed = discord.Embed(
        title='Coin flip', description=result, color=0xCD7F32)
    if coin:
        embed.set_thumbnail(
            url="http://news.coinupdate.com/wp-content/uploads/2017/02/1-pound-coin-obverse-2017.png")
    else:
        embed.set_thumbnail(
            url="https://www.royalmint.com/globalassets/__rebrand/_structure/new-one-pound/new-one-pound-reverse.png")
    await ctx.send(embed=embed)


@bot.command(name="8ball", usage='[question]')
async def _8ball(ctx, *, arg):
    replies = [
        "It is certain.",
        "It is decidedly so.",
        "Without a doubt.",
        "Yes - definitely.",
        "You may rely on it.",
        "As I see it, yes.",
        "Most likely.",
        "Outlook good.",
        "Yes.",
        "Signs point to yes.",
        "Reply hazy, try again.",
        "Ask again later.",
        "Better not tell you now.",
        "Cannot predict now.",
        "Concentrate and ask again.",
        "Don't count on it.",
        "My reply is no.",
        "My sources say no.",
        "Outlook not so good.",
        "Very doubtful.", ]
    result = random.randrange(len(replies))
    embed = discord.Embed(title="8 Ball", color=0xCD7F32)
    embed.add_field(name=ctx.author.name+"'s question:",
                    value=arg, inline=False)
    embed.add_field(name="Answer:", value=replies[result], inline=False)
    await ctx.send(embed=embed)


@bot.command()
async def repeat(ctx, *, arg):
    await ctx.send(arg)


@bot.command()
async def _repeat(ctx, cnt: int, *, arg):
    for i in range(cnt):
        await ctx.send(arg)

# Declares slash commands through the client.
slash = SlashCommand(bot, sync_commands=True)


@slash.slash(name="react", description="react to a message", guild_ids=bot.guilds, options=[
    create_option(
        name="id",
        description="Message ID",
        option_type=3,
        required=True
    ),
    create_option(
        name="emote",
        description="Emote",
        option_type=3,
        required=True
    ),
    create_option(
        name="channel",
        description="Message Channel",
        option_type=7,
        required=False
    ),
])
async def react(ctx, id, emote: discord.Emoji, channel=None):
    if not channel:
        channel = ctx.channel or ctx.author
    msg = await channel.fetch_message(id)
    if emote[0] == ':' or emote[0] == '<':
        emote = emote.split(':')[1]
    e = None
    if ctx.guild:
        em = ems.setdefault(ctx.guild.id, EmoteManager(ctx.guild))
        if em.eCycle:
            await em.addEmote(emote, ctx.guild)
        e = discord.utils.get(ctx.guild.emojis, name=emote)
    if not e:
        e = discord.utils.get(bot.emojis, name=emote)
    if not e:
        e = emoji.emojize(':'+emote+':', use_aliases=True)
    await msg.add_reaction(e)
    print('Reacted', emote)


bot.run(DISCORD_TOKEN)
