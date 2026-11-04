#!/usr/bin/env python3
"""CoganSU: check launch resources and dark-palette readability without an Android SDK."""
from pathlib import Path
import re
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'manager/app/src/main'


def luminance(rgb):
    channels = [(rgb >> shift & 255) / 255 for shift in (16, 8, 0)]
    channels = [c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4 for c in channels]
    return sum(c * w for c, w in zip(channels, (0.2126, 0.7152, 0.0722)))


class CoganAppearanceTest(unittest.TestCase):
    def test_launch_is_black_in_both_system_modes(self):
        for mode in ('values', 'values-night'):
            styles = {s.attrib['name']: {i.attrib['name']: i.text for i in s}
                      for s in ET.parse(ROOT / f'res/{mode}/themes.xml').getroot()}
            starting = styles['Theme.KernelSU.Starting']
            self.assertEqual(starting['windowSplashScreenBackground'], '@android:color/black')
            self.assertEqual(starting['windowSplashScreenAnimationDuration'], '0')
            self.assertEqual(starting['android:windowLightStatusBar'], 'false')
            self.assertEqual(styles['Theme.KernelSU']['android:windowBackground'], '@android:color/black')

    def test_text_contrast_on_cogan_panels(self):
        source = (ROOT / 'java/me/weishu/kernelsu/cogan/CoganTheme.kt').read_text()
        colors = {name: int(value, 16) & 0xFFFFFF for name, value in
                  re.findall(r'(\w+) = Color\(0x([A-Fa-f0-9]+)\)', source)}
        colors.update({name: 0 for name in re.findall(r'(\w+) = Color.Black', source)})
        pairs = [(f'on{name[0].upper()}{name[1:]}', name)
                 for name in ('primary', 'primaryContainer', 'secondary', 'secondaryContainer',
                              'tertiary', 'tertiaryContainer', 'surface', 'surfaceVariant', 'background')]
        pairs += [('onSurface', name) for name in colors if name.startswith('surfaceContainer')]
        for foreground, background in pairs:
            high, low = sorted((luminance(colors[foreground]), luminance(colors[background])), reverse=True)
            self.assertGreaterEqual((high + 0.05) / (low + 0.05), 4.5, (foreground, background))

    def test_cogan_labels_exist_in_both_languages(self):
        for locale in ('values', 'values-in'):
            strings = {s.attrib['name']: s.text for s in
                       ET.parse(ROOT / f'res/{locale}/strings_cogan.xml').getroot()}
            self.assertTrue(strings['cogan_style_masculine'])


if __name__ == '__main__':
    unittest.main()
