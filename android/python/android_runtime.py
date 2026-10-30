"""Run Nathan's engine and loopback API on the phone, with native key storage."""
import os
import webbrowser

_server = None
_url = None


def start(home: str) -> str:
    global _server, _url
    if _server is not None and _server.started:
        return _url
    from java import jclass
    vault = jclass("com.nathan.android.KeyVault")
    platform = jclass("com.nathan.android.MainActivity")
    os.environ.update(NATHAN_DATA=home + "/data", NATHAN_WORKSPACE=home + "/workspace",
                      NATHAN_KEYCHAIN_ONLY="1")
    from nathan import keys

    def load(provider):
        value = vault.get(provider)
        return str(value) if value else None

    def save(provider, key_env, key, **kwargs):
        vault.put(provider, key.strip())
        keys.clear_cache()
        return "keychain"

    def delete(provider, key_env, **kwargs):
        vault.remove(provider)
        keys.clear_cache()

    keys._keychain_get = load
    keys.keychain_available = lambda: True
    keys.set_key = save
    keys.delete_key = delete
    webbrowser.open = lambda url, **kwargs: platform.openExternal(str(url))

    from nathan.desktop import start_server
    from nathan.engine import Engine

    engine = Engine()
    _server, _url = start_server(engine, self_check=False)
    _server.config.app.state.open_browser = webbrowser.open
    return _url + "/#console"


def stop():
    if _server:
        _server.should_exit = True
