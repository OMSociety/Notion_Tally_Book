#!/bin/bash
cd /AstrBot/data/workspaces/Notion_Tally_Book
while IFS= read -r file; do
  perl -i -pe '
s/android\.arch\.persistence\.room\.migration\.Migration/androidx.room.migration.Migration/g;
s/android\.arch\.persistence\.db\.SupportSQLiteDatabase/androidx.sqlite.db.SupportSQLiteDatabase/g;
s/android\.arch\.persistence\.room\./androidx.room./g;
s/android\.arch\.lifecycle\./androidx.lifecycle./g;
s/android\.support\.test\.InstrumentationRegistry/androidx.test.platform.app.InstrumentationRegistry/g;
s/android\.support\.test\.runner\.AndroidJUnit4/androidx.test.ext.junit.runners.AndroidJUnit4/g;
s/android\.support\.v4\.app\.FragmentPagerAdapter/androidx.fragment.app.FragmentPagerAdapter/g;
s/android\.support\.v4\.app\.FragmentActivity/androidx.fragment.app.FragmentActivity/g;
s/android\.support\.v4\.app\.FragmentManager/androidx.fragment.app.FragmentManager/g;
s/android\.support\.v4\.app\.DialogFragment/androidx.fragment.app.DialogFragment/g;
s/android\.support\.v4\.app\.Fragment/androidx.fragment.app.Fragment/g;
s/android\.support\.v4\.app\.NotificationCompat/androidx.core.app.NotificationCompat/g;
s/android\.support\.v4\.app\.ActivityOptionsCompat/androidx.core.app.ActivityOptionsCompat/g;
s/android\.support\.v4\.app\.ActivityCompat/androidx.core.app.ActivityCompat/g;
s/android\.support\.v4\.content\.FileProvider/androidx.core.content.FileProvider/g;
s/android\.support\.v4\.content\.ContextCompat/androidx.core.content.ContextCompat/g;
s/android\.support\.v4\.view\.animation\.FastOutSlowInInterpolator/androidx.interpolator.view.animation.FastOutSlowInInterpolator/g;
s/android\.support\.v4\.view\.ViewPager\.SCROLL_STATE_DRAGGING/androidx.viewpager.widget.ViewPager.SCROLL_STATE_DRAGGING/g;
s/android\.support\.v4\.view\.ViewPager\.SCROLL_STATE_IDLE/androidx.viewpager.widget.ViewPager.SCROLL_STATE_IDLE/g;
s/android\.support\.v4\.view\.ViewPager\.SCROLL_STATE_SETTLING/androidx.viewpager.widget.ViewPager.SCROLL_STATE_SETTLING/g;
s/android\.support\.v4\.view\.ViewPager/androidx.viewpager.widget.ViewPager/g;
s/android\.support\.v4\.view\.PagerAdapter/androidx.viewpager.widget.PagerAdapter/g;
s/android\.support\.v4\.view\.GravityCompat/androidx.core.view.GravityCompat/g;
s/android\.support\.v4\.view\.PointerIconCompat/androidx.core.view.PointerIconCompat/g;
s/android\.support\.v4\.view\.ViewCompat/androidx.core.view.ViewCompat/g;
s/android\.support\.v4\.widget\.TextViewCompat/androidx.core.widget.TextViewCompat/g;
s/android\.support\.v4\.util\.Pools/androidx.core.util.Pools/g;
s/android\.support\.v4\.provider\.DocumentFile/androidx.documentfile.provider.DocumentFile/g;
s/android\.support\.v7\.app\.AppCompatDelegate/androidx.appcompat.app.AppCompatDelegate/g;
s/android\.support\.v7\.app\.AppCompatActivity/androidx.appcompat.app.AppCompatActivity/g;
s/android\.support\.v7\.app\.AlertDialog/androidx.appcompat.app.AlertDialog/g;
s/android\.support\.v7\.app\.ActionBar/androidx.appcompat.app.ActionBar/g;
s/android\.support\.v7\.widget\.RecyclerView/androidx.recyclerview.widget.RecyclerView/g;
s/android\.support\.v7\.widget\.LinearLayoutManager/androidx.recyclerview.widget.LinearLayoutManager/g;
s/android\.support\.v7\.widget\.GridLayoutManager/androidx.recyclerview.widget.GridLayoutManager/g;
s/android\.support\.v7\.widget\.DiffUtil/androidx.recyclerview.widget.DiffUtil/g;
s/android\.support\.v7\.widget\.ItemTouchHelper/androidx.recyclerview.widget.ItemTouchHelper/g;
s/android\.support\.v7\.widget\.Toolbar/androidx.appcompat.widget.Toolbar/g;
s/android\.support\.v7\.widget\.AppCompatImageView/androidx.appcompat.widget.AppCompatImageView/g;
s/android\.support\.v7\.widget\.AppCompatEditText/androidx.appcompat.widget.AppCompatEditText/g;
s/android\.support\.v7\.widget\.ListPopupWindow/androidx.appcompat.widget.ListPopupWindow/g;
s/android\.support\.v7\.widget\.TooltipCompat/androidx.appcompat.widget.TooltipCompat/g;
s/android\.support\.v7\.content\.res\.AppCompatResources/androidx.appcompat.content.res.AppCompatResources/g;
s/android\.support\.v7\.util\.DiffUtil/androidx.recyclerview.widget.DiffUtil/g;
s/android\.support\.v7\.appcompat\.R/androidx.appcompat.R/g;
s/android\.support\.design\.widget\.TabLayout/com.google.android.material.tabs.TabLayout/g;
s/android\.support\.design\.widget\.FloatingActionButton/com.google.android.material.floatingactionbutton.FloatingActionButton/g;
s/android\.support\.annotation\.Nullable/androidx.annotation.Nullable/g;
s/android\.support\.annotation\.NonNull/androidx.annotation.NonNull/g;
s/android\.support\.annotation\.Keep/androidx.annotation.Keep/g;
s/android\.support\.annotation\.StringRes/androidx.annotation.StringRes/g;
s/android\.support\.annotation\.DrawableRes/androidx.annotation.DrawableRes/g;
s/android\.support\.annotation\.ColorRes/androidx.annotation.ColorRes/g;
s/android\.support\.annotation\.ColorInt/androidx.annotation.ColorInt/g;
s/android\.support\.annotation\.LayoutRes/androidx.annotation.LayoutRes/g;
s/android\.support\.annotation\.AttrRes/androidx.annotation.AttrRes/g;
s/android\.support\.annotation\.StyleRes/androidx.annotation.StyleRes/g;
s/android\.support\.annotation\.IntDef/androidx.annotation.IntDef/g;
s/android\.support\.annotation\.IntegerRes/androidx.annotation.IntegerRes/g;
s/android\.support\.annotation\.RestrictTo\.Scope\.LIBRARY_GROUP/androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP/g;
s/android\.support\.annotation\.RestrictTo/androidx.annotation.RestrictTo/g;
' "$file"
done < /AstrBot/data/workspaces/Notion_Tally_Book/files_to_migrate.txt

echo "Migration complete. Checking for remaining old imports..."
remaining=$(grep -rn "android\.support\.\|android\.arch\." --include="*.java" /AstrBot/data/workspaces/Notion_Tally_Book/ 2>/dev/null | wc -l)
echo "Remaining references: $remaining"
if [ "$remaining" -gt 0 ]; then
  grep -rn "android\.support\.\|android\.arch\." --include="*.java" /AstrBot/data/workspaces/Notion_Tally_Book/ 2>/dev/null
fi
