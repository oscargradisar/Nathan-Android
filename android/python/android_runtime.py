"""Run Nathan's engine and loopback API on the phone, with native key storage."""
import os
import threading
import time
import webbrowser

_server = None
_url = None


def start(home: str) -> str:
    global _server, _url
    if _server is not None and _server.started and not _server.should_exit:
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
    def watch_exit():
        while not _server.should_exit:
            if _server.config.app.state.quit:
                platform.finishApp()
                return
            time.sleep(0.5)

    threading.Thread(target=watch_exit, daemon=True).start()
    return _url + "/#console"


def stop():
    if _server:
        _server.should_exit = True


def self_check():
    """Check the Android-pinned SDK path without contacting any provider."""
    import asyncio

    import httpx

    from nathan.client import Client
    from nathan.config import Provider

    async def run():
        def respond(request):
            return httpx.Response(200, json={"id": "test", "object": "chat.completion",
                "created": 0, "model": "test", "choices": [{"index": 0,
                "message": {"role": "assistant", "content": "Android worker OK"},
                "finish_reason": "stop"}]})

        provider = Provider(name="test", base_url="https://example.test/v1", models={"test": {}})
        client = Client({"test": provider}, http_client=httpx.AsyncClient(
            transport=httpx.MockTransport(respond)))
        try:
            result = await client.chat("test/test", [{"role": "user", "content": "test"}])
            assert result.text == "Android worker OK"
            return result.text
        finally:
            await client.close()

    return asyncio.run(run())
