"""The public search UI runs in a native hidden WebView, not desktop Playwright."""
import asyncio
import json


async def cameras(place: str) -> list[dict]:
    from java import jclass
    raw = await asyncio.to_thread(jclass("com.nathan.android.CameraLookup").search,
                                  place.split(",")[0].strip())
    result = json.loads(str(raw))
    if "error" in result:
        raise ValueError(result["error"])
    return result.get("cameras", [])[:50]
