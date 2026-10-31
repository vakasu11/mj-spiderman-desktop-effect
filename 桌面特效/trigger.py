"""Small in-memory trigger buffer. Never stores or logs full keyboard input."""
import time


class TriggerMatcher:
    def __init__(self, mode='instant', timeout=3.0):
        self.mode = mode
        self.timeout = timeout
        self.reset()

    def reset(self):
        self.buffer = ''
        self.last_time = 0.0
        self.context = None

    def feed(self, key, *, enabled=True, context=None, now=None):
        now = time.monotonic() if now is None else now
        if context != self.context or now - self.last_time > self.timeout:
            self.buffer = ''
        self.context, self.last_time = context, now
        if key == 'enter':
            result = 'quit' if self.buffer.endswith('stopmj') else (
                'go' if enabled and self.mode == 'enter' and self.buffer.endswith('mj') else None)
            self.buffer = ''
            return result
        if key == 'backspace':
            self.buffer = self.buffer[:-1]
            return None
        if len(key) != 1 or not key.isprintable():
            self.buffer = ''
            return None
        self.buffer = (self.buffer + key.lower())[-8:]
        if self.buffer.endswith('stopmj'):
            return None  # Quit command must not play an animation first.
        if enabled and self.mode == 'instant' and self.buffer.endswith('mj'):
            self.buffer = ''
            return 'go'
        return None
