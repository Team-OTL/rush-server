#!/usr/bin/env python3
import argparse
import json
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
GRADLE_SHARED = ("build-logic/", "gradle/", "build.gradle.kts", "settings.gradle.kts", "gradlew", "gradlew.bat")
ALL_SHARED = (".github/", ".editorconfig")


def discover_modules() -> dict[str, str]:
    modules = {}
    for path in sorted(ROOT.glob("rush-*")):
        if (path / "build.gradle.kts").exists():
            modules[path.name] = "kotlin"
        elif (path / "package.json").exists():
            modules[path.name] = "nest"
        elif (path / "go.mod").exists():
            modules[path.name] = "go"
    return modules


def commit_exists(sha: str) -> bool:
    if not sha or set(sha) == {"0"}:
        return False
    result = subprocess.run(["git", "cat-file", "-e", f"{sha}^{{commit}}"], cwd=ROOT, capture_output=True)
    return result.returncode == 0


def changed_files(base: str, head: str) -> list[str]:
    out = subprocess.run(
        ["git", "diff", "--name-only", f"{base}...{head}"],
        cwd=ROOT,
        check=True,
        capture_output=True,
        text=True,
    ).stdout
    return [line for line in out.splitlines() if line]


def select(modules: dict[str, str], files: list[str] | None) -> list[str]:
    if files is None:
        return list(modules)

    selected: set[str] = set()
    for file in files:
        if file.startswith(ALL_SHARED):
            return list(modules)
        if file.startswith(GRADLE_SHARED):
            selected.update(name for name, stack in modules.items() if stack == "kotlin")
            continue
        top = file.split("/", 1)[0]
        if top in modules:
            selected.add(top)
    return [name for name in modules if name in selected]


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--base", default="")
    parser.add_argument("--head", default="HEAD")
    args = parser.parse_args()

    modules = discover_modules()
    files = changed_files(args.base, args.head) if commit_exists(args.base) else None
    targets = select(modules, files)

    matrix = {"include": [{"service": name, "stack": modules[name]} for name in targets]}
    services = ", ".join(targets) if targets else "없음"
    print(f"targets: {services}")

    output = os.environ.get("GITHUB_OUTPUT")
    if output:
        with open(output, "a", encoding="utf-8") as fh:
            fh.write(f"matrix={json.dumps(matrix, separators=(',', ':'))}\n")
            fh.write(f"services-list={services}\n")


if __name__ == "__main__":
    main()
