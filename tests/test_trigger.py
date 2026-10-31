import sys
import unittest
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / '桌面特效'))
from trigger import TriggerMatcher


class TriggerTests(unittest.TestCase):
    def type_keys(self, matcher, keys, **kwargs):
        return [a for k in keys if (a := matcher.feed(k, **kwargs))]

    def test_immediate_and_repeat(self):
        self.assertEqual(self.type_keys(TriggerMatcher(), 'mjmj'), ['go', 'go'])

    def test_case_insensitive(self):
        self.assertEqual(self.type_keys(TriggerMatcher(), 'MJ'), ['go'])

    def test_return_mode(self):
        m = TriggerMatcher('enter')
        self.assertEqual(self.type_keys(m, 'mj'), [])
        self.assertEqual(m.feed('enter'), 'go')
        self.assertIsNone(m.feed('enter'))

    def test_immediate_no_double_on_enter(self):
        m = TriggerMatcher()
        self.assertEqual(self.type_keys(m, ['m', 'j', 'enter']), ['go'])

    def test_exit_no_animation_in_both_modes(self):
        for mode in ['instant', 'enter']:
            m = TriggerMatcher(mode)
            self.assertEqual(self.type_keys(m, 'stopmj'), [])
            self.assertEqual(m.feed('enter'), 'quit')

    def test_pause_and_quit(self):
        m = TriggerMatcher()
        self.assertEqual(self.type_keys(m, 'mj', enabled=False), [])
        self.type_keys(m, 'stopmj', enabled=False)
        self.assertEqual(m.feed('enter', enabled=False), 'quit')

    def test_edit_correction(self):
        self.assertEqual(self.type_keys(TriggerMatcher(), ['m', 'x', 'backspace', 'j']), ['go'])

    def test_navigation_cancels_prefix(self):
        self.assertEqual(self.type_keys(TriggerMatcher(), ['m', 'reset', 'j']), [])

    def test_window_switch_cancels_prefix(self):
        m = TriggerMatcher()
        m.feed('m', context=100)
        self.assertIsNone(m.feed('j', context=200))

    def test_stale_prefix_cancels(self):
        m = TriggerMatcher()
        m.feed('m', now=1)
        self.assertIsNone(m.feed('j', now=5))

    def test_buffer_bounded(self):
        m = TriggerMatcher()
        self.type_keys(m, 'abcdefghijklmnop' * 100)
        self.assertLessEqual(len(m.buffer), 8)

    def test_mode_change_resets(self):
        m = TriggerMatcher()
        m.feed('m')
        m.mode = 'enter'
        m.reset()
        self.assertEqual(self.type_keys(m, ['j', 'enter']), [])


if __name__ == '__main__':
    unittest.main()
