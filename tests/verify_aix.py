"""Check App Inventor metadata, permission, runtime classes, and Android DEX integrity."""
import hashlib
import io
import json
from pathlib import Path
import struct
from zipfile import ZipFile
import zlib

root = Path(__file__).resolve().parents[1]
with ZipFile(root / 'dist/SoundLevel.aix') as aix:
    assert aix.testzip() is None
    prefix = 'org.classroom.soundlevel/'
    component = json.loads(aix.read(prefix + 'components.json'))[0]
    assert component['type'] == 'org.classroom.soundlevel.SoundLevel'
    assert component['version'] == '4'
    assert {m['name'] for m in component['methods']} == {'Start', 'Stop'}
    events = {e['name']: e for e in component['events']}
    assert events['LevelChanged']['params'] == [{'name': 'level', 'type': 'number'}]
    assert [(p['name'], p['type']) for p in events['ColorChanged']['params']] == [
        ('red', 'number'), ('green', 'number'), ('blue', 'number'), ('color', 'number')]
    assert [(p['name'], p['type']) for p in events['SpectrumChanged']['params']] == [
        ('low', 'number'), ('mid', 'number'), ('high', 'number'),
        ('red', 'number'), ('green', 'number'), ('blue', 'number'), ('color', 'number')]
    defaults = {p['name']: p['defaultValue'] for p in component['properties']}
    assert defaults['AnalysisMode'] == 'Loudness' and defaults['NoiseGate'] == '20'
    assert defaults['ColorTheme'] == 'RGB' and defaults['SilenceColor'] == '&HFF000000'
    assert next(p for p in component['properties'] if p['name'] == 'SilenceColor')['editorType'] == 'color'
    assert {'Error', 'Started', 'Stopped'} <= events.keys()
    permissions = json.loads(aix.read(prefix + 'files/component_build_infos.json'))[0]['permissions']
    assert permissions == ['android.permission.RECORD_AUDIO'], permissions
    with ZipFile(io.BytesIO(aix.read(prefix + 'files/AndroidRuntime.jar'))) as jar:
        assert 'org/classroom/soundlevel/SoundLevel.class' in jar.namelist()
        assert 'org/classroom/soundlevel/helpers/LevelMath.class' in jar.namelist()
        assert 'org/classroom/soundlevel/helpers/SpectrumAnalyzer.class' in jar.namelist()
        assert 'org/classroom/soundlevel/helpers/SpectrumAnalyzer$Result.class' in jar.namelist()
        assert 'org/classroom/soundlevel/helpers/SpectrumColors.class' in jar.namelist()
        assert not any(n.startswith(('android/', 'com/google/appinventor/')) for n in jar.namelist())
    with ZipFile(io.BytesIO(aix.read(prefix + 'classes.jar'))) as jar:
        dex = jar.read('classes.dex')
        assert dex[:4] == b'dex\n'
        assert struct.unpack_from('<I', dex, 32)[0] == len(dex)
        assert dex[12:32] == hashlib.sha1(dex[32:]).digest()
        assert struct.unpack_from('<I', dex, 8)[0] == zlib.adler32(dex[12:])
        assert b'Lorg/classroom/soundlevel/SoundLevel;' in dex
        assert b'Lorg/classroom/soundlevel/helpers/LevelMath;' in dex
        assert b'Lorg/classroom/soundlevel/helpers/SpectrumAnalyzer;' in dex
        assert b'Lorg/classroom/soundlevel/helpers/SpectrumAnalyzer$Result;' in dex
        assert b'Lorg/classroom/soundlevel/helpers/SpectrumColors;' in dex
print('PASS: AIX archive, block metadata, microphone-only permission, JVM classes, DEX integrity')
