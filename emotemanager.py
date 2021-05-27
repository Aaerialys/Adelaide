from emote import Emote
import time
import os
import requests

from discord.ext import tasks


class EmoteManager:
    def __init__(self, server):
        self.eCycle = False
        self.eReplace = "off"
        self.eList = {}
        self.eDir = os.getcwd()+'\\emotes\\'+str(server.id)+'\\'
        if not os.path.exists(self.eDir):
            os.makedirs(self.eDir)

    async def update(self, server):
        self.updCacheFromFiles()
        self.updCacheFromServer(server)

    def updCacheFromFiles(self):
        for file in os.listdir(self.eDir):
            name = os.path.splitext(file)[0]
            if not name in self.eList:
                self.eList[name] = Emote(file, name)

    def updCacheFromServer(self, server):
        for e in self.eList.values():
            e.inServer = False
        for e in server.emojis:
            if e.name not in self.eList:
                path = e.name+(".gif" if e.animated else ".png")
                open(self.eDir+path, 'wb').write(requests.get(e.url).content)
                self.eList[e.name] = Emote(path, e.name)
            self.eList[e.name].inServer = True

    def getServerMax(self, server):
        serverMax = server.premium_subscription_count
        serverMax = 50*(serverMax+1+(serverMax == 3))
        return serverMax

    async def addEmote(self, s, server):
        if s in self.eList:
            try:
                cur = self.eList[s]
                cur.addOc()
                if cur.inServer:
                    return False
                cnt = 0
                for e in server.emojis:
                    cnt += (e.animated == self.eList[s].animated)
                if cnt >= self.getServerMax(server):
                    print(server.name, "is full with",
                          cnt, "emotes")
                    await self.removeLastEmote(e.animated,server)
                await server.create_custom_emoji(name=cur.name, image=open(self.eDir+cur.file, 'rb').read())
                cur.inServer = True
                cur.cycleable = True
                print('Added', s)
                return True
            except Exception as e:
                print('Error adding ', s, '\n', e)
        return False

    async def removeLastEmote(self, animated, server):
        last = None
        for e in server.emojis:
            if e.name in self.eList and self.eList[e.name].cycleable and e.animated==animated\
                    and (not last or self.eList[last.name].lastOc > self.eList[e.name].lastOc):
                last = e
        if last:
            self.eList[last.name].inServer = False
            await last.delete()
            print('Deleted', last.name)
