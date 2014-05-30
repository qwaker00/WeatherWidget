import time

class Weather(object):
    def __init__(self, city):
        raise "Not implemented" 

    def get(self):
        raise "Not implemented" 

    lastupdate = 0
    cached = None

    def get_with_cache(self):
        t = time.time()
        if t - self.lastupdate > 60:
            self.cached = self.get()
            self.lastupdate = t
        return self.cached
