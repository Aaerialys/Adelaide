import time
import os


class Emote:
    def __init__(self, file, name):
        self.file = file
        self.name = name
        self.lastOc = 0
        self.freq = 0
        self.inServer = False
        self.cycleable = False
        self.animated = self.file.endswith('.gif')

    def addOc(self):
        self.lastOc = time.time()
        self.freq += 1
