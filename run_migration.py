#!/usr/bin/env python3
import os

BASE = "/AstrBot/data/workspaces/Notion_Tally_Book"

REPLACEMENTS = [
    ("android.arch.persistence.room.migration.Migration", "androidx.room.migration.Migration"),
    ("android.arch.persistence.db.SupportSQLiteDatabase", "androidx.sqlite.db.SupportSQLiteDatabase"),
    ("android.arch.persistence.room.", "androidx.room."),
    ("android.arch.lifecycle.", "androidx.lifecycle."),
    ("android.support.test.InstrumentationRegistry", "androidx.test.platform.app.InstrumentationRegistry"),
    ("android.support.test.runner.AndroidJUnit4", "androidx.test.ext.junit.runners.AndroidJUnit4"),
    ("android.support.v4.app.FragmentPagerAdapter", "androidx.fragment.app.FragmentPagerAdapter"),
    ("android.support.v4.app.FragmentActivity", "androidx.fragment.app.FragmentActivity"),
    ("android.support.v4.app.FragmentManager", "androidx.fragment.app.FragmentManager"),
    ("android.support.v4.app.DialogFragment", "androidx.fragment.app.DialogFragment"),
    ("android.support.v4.app.Fragment", "androidx.fragment.app.Fragment"),
    ("android.support.v4.app.NotificationCompat", "androidx.core.app.NotificationCompat"),
    ("android.support.v4.app.ActivityOptionsCompat", "androidx.core.app.ActivityOptionsCompat"),
    ("android.support.v4.app.ActivityCompat", "androidx.core.app.ActivityCompat"),
    ("android.support.v4.content.FileProvider", "androidx.core.content.FileProvider"),
    ("android.support.v4.content.ContextCompat", "androidx.core.content.ContextCompat"),
    ("android.support.v4.view.animation.FastOutSlowInInterpolator", "androidx.interpolator.view.animation.FastOutSlowInInterpolator"),
    ("android.support.v4.view.ViewPager.SCROLL_STATE_DRAGGING", "androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING"),
    ("android.support.v4.view.ViewPager.SCROLL_STATE_IDLE", "androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE"),
    ("android.support.v4.view.ViewPager.SCROLL_STATE_SETTLING", "androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING"),
    ("android.support.v4.view.ViewPager", "androidx.viewpager.widget.ViewPager"),
    ("android.support.v4.view.PagerAdapter", "androidx.viewpager.widget.PagerAdapter"),
    ("android.support.v4.view.GravityCompat", "androidx.core.view.GravityCompat"),
    ("android.support.v4.view.PointerIconCompat", "androidx.core.view.PointerIconCompat"),
    ("android.support.v4.view.ViewCompat", "androidx.core.view.ViewCompat"),
    ("android.support.v4.widget.TextViewCompat", "androidx.core.widget.TextViewCompat"),
    ("android.support.v4.util.Pools", "androidx.core.util.Pools"),
    ("android.support.v4.provider.DocumentFile", "androidx.documentfile.provider.DocumentFile"),
    ("android.support.v7.app.AppCompatDelegate", "androidx.appcompat.app.AppCompatDelegate"),
    ("android.support.v7.app.AppCompatActivity", "androidx.appcompat.app.AppCompatActivity"),
    ("android.support.v7.app.AlertDialog", "androidx.appcompat.app.AlertDialog"),
    ("android.support.v7.app.ActionBar", "androidx.appcompat.app.ActionBar"),
    ("android.support.v7.widget.RecyclerView", "androidx.recyclerview.widget.RecyclerView"),
    ("android.support.v7.widget.LinearLayoutManager", "androidx.recyclerview.widget.LinearLayoutManager"),
    ("android.support.v7.widget.GridLayoutManager", "androidx.recyclerview.widget.GridLayoutManager"),
    ("android.support.v7.widget.DiffUtil", "androidx.recyclerview.widget.DiffUtil"),
    ("android.support.v7.widget.ItemTouchHelper", "androidx.recyclerview.widget.ItemTouchHelper"),
    ("android.support.v7.widget.Toolbar", "androidx.appcompat.widget.Toolbar"),
    ("android.support.v7.widget.AppCompatImageView", "androidx.appcompat.widget.AppCompatImageView"),
    ("android.support.v7.widget.AppCompatEditText", "androidx.appcompat.widget.AppCompatEditText"),
    ("android.support.v7.widget.ListPopupWindow", "androidx.appcompat.widget.ListPopupWindow"),
    ("android.support.v7.widget.TooltipCompat", "androidx.appcompat.widget.TooltipCompat"),
    ("android.support.v7.content.res.AppCompatResources", "androidx.appcompat.content.res.AppCompatResources"),
    ("android.support.v7.util.DiffUtil", "androidx.recyclerview.widget.DiffUtil"),
    ("android.support.v7.appcompat.R", "androidx.appcompat.R"),
    ("android.support.design.widget.TabLayout", "com.google.android.material.tabs.TabLayout"),
    ("android.support.design.widget.FloatingActionButton", "com.google.android.material.floatingactionbutton.FloatingActionButton"),
    ("android.support.annotation.Nullable", "androidx.annotation.Nullable"),
    ("android.support.annotation.NonNull", "androidx.annotation.NonNull"),
    ("android.support.annotation.Keep", "androidx.annotation.Keep"),
    ("android.support.annotation.StringRes", "androidx.annotation.StringRes"),
    ("android.support.annotation.DrawableRes", "androidx.annotation.DrawableRes"),
    ("android.support.annotation.ColorRes", "androidx.annotation.ColorRes"),
    ("android.support.annotation.ColorInt", "androidx.annotation.ColorInt"),
    ("android.support.annotation.LayoutRes", "androidx.annotation.LayoutRes"),
    ("android.support.annotation.AttrRes", "androidx.annotation.AttrRes"),
    ("android.support.annotation.StyleRes", "androidx.annotation.StyleRes"),
    ("android.support.annotation.IntDef", "androidx.annotation.IntDef"),
    ("android.support.annotation.IntegerRes", "androidx.annotation.IntegerRes"),
    ("android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP", "androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP"),
    ("android.support.annotation.RestrictTo", "androidx.annotation.RestrictTo"),
]


def main():
    files_path = os.path.join(BASE, "files_to_migrate.txt")
    with open(files_path) as f:
        files = [line.strip() for line in f if line.strip()]

    print(f"Found {len(files)} files to migrate")

    changed = 0
    for fpath in files:
        with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
            content = f.read()
        original = content
        for old, new in REPLACEMENTS:
            content = content.replace(old, new)
        if content != original:
            with open(fpath, "w", encoding="utf-8") as f:
                f.write(content)
            changed += 1
            rel = os.path.relpath(fpath, BASE)
            print(f"  migrated: {rel}")

    print(f"\nMigrated {changed}/{len(files)} files")

    remaining = 0
    for root, dirs, filenames in os.walk(BASE):
        dirs[:] = [d for d in dirs if d not in ("build", ".gradle", ".git")]
        for fname in filenames:
            if fname.endswith(".java"):
                fpath = os.path.join(root, fname)
                with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                    for i, line in enumerate(f, 1):
                        if "android.support." in line or "android.arch." in line:
                            rel = os.path.relpath(fpath, BASE)
                            print(f"  REMAINING: {rel}:{i}: {line.rstrip()}")
                            remaining += 1

    if remaining:
        print(f"\nWARNING: {remaining} lines with old imports remain")
    else:
        print("\nAll old imports have been successfully migrated!")


if __name__ == "__main__":
    main()
