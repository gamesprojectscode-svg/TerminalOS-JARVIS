import contextlib
import io
import ast

SAFE_BUILTINS = {
    "print": print, "len": len, "range": range, "str": str, "int": int,
    "float": float, "bool": bool, "list": list, "dict": dict, "tuple": tuple,
    "set": set, "sum": sum, "min": min, "max": max, "abs": abs, "round": round,
    "sorted": sorted, "enumerate": enumerate, "zip": zip, "type": type
}

def run_code(code):
    output = io.StringIO()
    env = {"__builtins__": SAFE_BUILTINS}
    try:
        with contextlib.redirect_stdout(output):
            exec(compile(code, "<terminalos>", "exec"), env, env)
        return output.getvalue() or "Código executado sem saída."
    except Exception as exc:
        return output.getvalue() + type(exc).__name__ + ": " + str(exc)
