package tw.nekomimi.nekogram.settings;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.view.MotionEvent;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.EditText;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.Utilities;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.SettingsSearchCell;
import org.telegram.ui.Components.CubicBezierInterpolator;
import org.telegram.ui.Components.FragmentFloatingButton;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.LaunchActivity;
import org.telegram.ui.ProfileActivity.SearchAdapter.SearchResult;

import java.util.ArrayList;
import java.util.List;

import me.vkryl.android.animator.BoolAnimator;
import me.vkryl.android.animator.FactorAnimator;
import tw.nekomimi.nekogram.accessibility.AccessibilitySettingsActivity;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;
import tw.nekomimi.nekogram.helpers.PasscodeHelper;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class NekoSettingsActivity extends BaseNekoSettingsActivity implements FactorAnimator.Target {

    private static final int ANIMATOR_ID_SEARCH_PAGE_VISIBLE = 0;

    private final BoolAnimator animatorSearchPageVisible = new BoolAnimator(ANIMATOR_ID_SEARCH_PAGE_VISIBLE,
            this, CubicBezierInterpolator.EASE_OUT_QUINT, 350);

    private final List<ConfigHelper.News> news = ConfigHelper.getNews();

    private final int generalRow = rowId++;
    private final int appearanceRow = rowId++;
    private final int chatRow = rowId++;
    private final int passcodeRow = rowId++;
    private final int experimentRow = rowId++;
    private final int accessibilityRow = rowId++;

    private final int channelRow = rowId++;
    private final int websiteRow = rowId++;
    private final int sourceCodeRow = rowId++;
    private final int translationRow = rowId++;
    private final int donateRow = rowId++;
    private final int checkUpdateRow = rowId++;

    private final int sponsorRow = 100;

    private ActionBarMenuItem searchItem, syncItem;
    private final ArrayList<SearchResult> searchArray = createSearchArray();
    private final ArrayList<CharSequence> resultNames = new ArrayList<>();
    private final ArrayList<SearchResult> searchResults = new ArrayList<>();
    private boolean searchWas;
    private Runnable searchRunnable;
    private String lastSearchString;

    @Override
    public View createView(Context context) {
        var fragmentView = super.createView(context);

        var menu = actionBar.createMenu();
        searchItem = menu.addItem(0, R.drawable.outline_header_search, resourceProvider).setIsSearchField(true).setActionBarMenuItemSearchListener(new ActionBarMenuItem.ActionBarMenuItemSearchListener() {

            @Override
            public void onSearchCollapse() {
                animatorSearchPageVisible.setValue(false, true);
                listView.adapter.update(true);
            }

            @Override
            public void onSearchExpand() {
                animatorSearchPageVisible.setValue(true, true);
                search("");
                listView.adapter.update(true);
            }

            @Override
            public void onTextChanged(EditText editText) {
                search(editText.getText().toString());
            }
        });
        searchItem.setSearchFieldHint(getString(R.string.Search));
        searchItem.setContentDescription(getString(R.string.Search));
        syncItem = menu.addItem(1, R.drawable.cloud_sync);
        syncItem.setOnClickListener(v -> CloudSettingsHelper.getInstance().showDialog(this));

        return fragmentView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        if (searchItem != null && searchItem.isSearchFieldVisible2()) {
            fillSearchItems(items);
            return;
        }

        items.add(UItem.asHeader(LocaleController.getString(R.string.Categories)));
        items.add(UItem.asButton(generalRow, R.drawable.msg_media, LocaleController.getString(R.string.General)).slug("general"));
        items.add(UItem.asButton(appearanceRow, R.drawable.msg_theme, LocaleController.getString(R.string.ChangeChannelNameColor2)).slug("appearance"));
        items.add(UItem.asButton(chatRow, R.drawable.msg_discussion, LocaleController.getString(R.string.Chat)).slug("chat"));
        if (!PasscodeHelper.isSettingsHidden()) {
            items.add(UItem.asButton(passcodeRow, R.drawable.msg_secret, LocaleController.getString(R.string.PasscodeNeko)).slug("passcode"));
        }
        items.add(UItem.asButton(experimentRow, R.drawable.msg_fave, LocaleController.getString(R.string.NotificationsOther)).slug("experiment"));
        AccessibilityManager am = (AccessibilityManager) ApplicationLoader.applicationContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am != null && am.isTouchExplorationEnabled()) {
            items.add(UItem.asButton(accessibilityRow, LocaleController.getString(R.string.AccessibilitySettings)).slug("accessibility"));
        }
        items.add(UItem.asShadow(null));

        items.add(UItem.asHeader(LocaleController.getString(R.string.About)));
        items.add(TextSettingsCellFactory.of(channelRow, LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername)).slug("channel"));
        items.add(TextSettingsCellFactory.of(websiteRow, LocaleController.getString(R.string.OfficialSite), "nekogram.app").slug("website"));
        items.add(TextSettingsCellFactory.of(sourceCodeRow, LocaleController.getString(R.string.ViewSourceCode), "GitHub").slug("sourceCode"));
        items.add(TextDetailSettingsCellFactory.of(translationRow, LocaleController.getString(R.string.Translation), LocaleController.getString(R.string.TranslationAbout)).slug("translation"));
        items.add(TextDetailSettingsCellFactory.of(donateRow, LocaleController.getString(R.string.Donate), LocaleController.getString(R.string.DonateAbout)).slug("donate"));
        items.add(TextDetailSettingsCellFactory.of(checkUpdateRow, LocaleController.getString(R.string.CheckUpdate), UpdateHelper.formatDateUpdate(SharedConfig.lastUpdateCheckTime)).slug("checkUpdate"));
        items.add(UItem.asShadow(null));

        if (!news.isEmpty()) {
            var newsId = 0;
            for (var newsItem : news) {
                items.add(TextDetailSettingsCellFactory.of(sponsorRow + newsId++, newsItem.title, newsItem.summary));
            }
            items.add(UItem.asShadow(null));
        }
    }

    @Override
    protected void onItemClick(UItem item, View view, int position, float x, float y) {
        if (item.instanceOf(SettingsSearchCell.Factory.class)) {
            if (item.object instanceof SearchResult r) {
                r.open(null);
            }
            return;
        }
        var id = item.id;
        if (id == chatRow) {
            presentFragment(new NekoChatSettingsActivity());
        } else if (id == generalRow) {
            presentFragment(new NekoGeneralSettingsActivity());
        } else if (id == appearanceRow) {
            presentFragment(new NekoAppearanceSettingsActivity());
        } else if (id == passcodeRow) {
            presentFragment(new NekoPasscodeSettingsActivity());
        } else if (id == experimentRow) {
            presentFragment(new NekoExperimentalSettingsActivity());
        } else if (id == accessibilityRow) {
            presentFragment(new AccessibilitySettingsActivity());
        } else if (id == channelRow) {
            getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1);
        } else if (id == donateRow) {
            presentFragment(new NekoDonateActivity());
        } else if (id == translationRow) {
            Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram");
        } else if (id == websiteRow) {
            Browser.openUrl(getParentActivity(), "https://nekogram.app");
        } else if (id == sourceCodeRow) {
            Browser.openUrl(getParentActivity(), "https://github.com/Nekogram/Nekogram");
        } else if (id == checkUpdateRow) {
            ((LaunchActivity) getParentActivity()).checkAppUpdate(true, new Browser.Progress() {
                @Override
                public void end() {
                    item.subtext = UpdateHelper.formatDateUpdate(SharedConfig.lastUpdateCheckTime);
                    listView.adapter.notifyItemChanged(position);
                }
            });
            item.subtext = LocaleController.getString(R.string.CheckingUpdate);
            listView.adapter.notifyItemChanged(position);
        } else if (id >= sponsorRow) {
            var newsItem = news.get(id - sponsorRow);
            Browser.openUrl(getParentActivity(), newsItem.url);
        }
    }

    @Override
    protected String getActionBarTitle() {
        return LocaleController.getString(R.string.NekoSettings);
    }

    @Override
    protected String getKey() {
        return "";
    }

    @Override
    public boolean onBackPressed(boolean invoked) {
        if (searchItem.isSearchFieldVisible2()) {
            if (invoked) actionBar.closeSearchField();
            return false;
        }
        return super.onBackPressed(invoked);
    }

    @Override
    public void onFactorChanged(int id, float factor, float fraction, FactorAnimator callee) {
        if (id == ANIMATOR_ID_SEARCH_PAGE_VISIBLE) {
            FragmentFloatingButton.setAnimatedVisibility(syncItem, 1f - factor);
        }
    }

    @Override
    public boolean isSwipeBackEnabled(MotionEvent event) {
        return !animatorSearchPageVisible.getValue();
    }

    private static BaseNekoSettingsActivity createFragment(int icon) {
        if (icon == R.drawable.msg_media) {
            return new NekoGeneralSettingsActivity();
        } else if (icon == R.drawable.msg_theme) {
            return new NekoAppearanceSettingsActivity();
        } else if (icon == R.drawable.msg_discussion) {
            return new NekoChatSettingsActivity();
        } else if (icon == R.drawable.msg_fave) {
            return new NekoExperimentalSettingsActivity();
        }
        return new NekoSettingsActivity();
    }

    private ArrayList<SearchResult> createSearchArray() {
        var searchResultList = new ArrayList<SearchResult>();
        var icons = new int[]{
                R.drawable.msg_media,
                R.drawable.msg_theme,
                R.drawable.msg_discussion,
                R.drawable.msg_fave,
        };
        for (var i = 0; i < icons.length; i++) {
            var icon = icons[i];
            var fragment = createFragment(icon);
            var items = new ArrayList<UItem>();
            fragment.fillItems(items, null);
            var fragmentTitle = fragment.getActionBarTitle();
            String headerText = null;
            for (var item : items) {
                if (item.viewType == UniversalAdapter.VIEW_TYPE_HEADER) {
                    headerText = item.text.toString();
                    continue;
                } else if (item.viewType == UniversalAdapter.VIEW_TYPE_SHADOW) {
                    headerText = null;
                    continue;
                }
                if (TextUtils.isEmpty(item.slug)) continue;
                searchResultList.add(new SearchResult(i * 1000 + item.id, item.text.toString(), null, fragmentTitle, fragmentTitle.equals(headerText) ? null : headerText, icon, () -> {
                    var fragment1 = createFragment(icon);
                    presentFragment(fragment1);
                    AndroidUtilities.runOnUIThread(() -> fragment1.scrollToRow(item.slug, () -> {
                    }));
                }));
            }
            searchResultList.add(new SearchResult(10000 + i, fragmentTitle, icon, () -> presentFragment(fragment)));
        }
        searchResultList.add(new SearchResult(8000, LocaleController.getString(R.string.EmojiUseDefault), null, LocaleController.getString(R.string.Chat), LocaleController.getString(R.string.EmojiSets), R.drawable.msg_theme, () -> {
            var fragment = new NekoEmojiSettingsActivity();
            presentFragment(fragment);
            AndroidUtilities.runOnUIThread(() -> fragment.scrollToRow("useSystemEmoji", () -> {
            }));
        }));

        searchResultList.add(new SearchResult(20000, LocaleController.getString(R.string.OfficialChannel), "@" + LocaleController.getString(R.string.OfficialChannelUsername), R.drawable.msg2_help, () -> getMessagesController().openByUserName(LocaleController.getString(R.string.OfficialChannelUsername), this, 1)));
        searchResultList.add(new SearchResult(20001, LocaleController.getString(R.string.OfficialSite), "nekogram.app", R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://nekogram.app")));
        searchResultList.add(new SearchResult(20002, LocaleController.getString(R.string.ViewSourceCode), "GitHub", R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://github.com/Nekogram/Nekogram")));
        searchResultList.add(new SearchResult(20003, LocaleController.getString(R.string.Translation), LocaleController.getString(R.string.TranslationAbout), R.drawable.msg2_help, () -> Browser.openUrl(getParentActivity(), "https://neko.crowdin.com/nekogram")));
        searchResultList.add(new SearchResult(20004, LocaleController.getString(R.string.Donate), LocaleController.getString(R.string.DonateAbout), R.drawable.msg2_help, () -> presentFragment(new NekoDonateActivity())));

        return searchResultList;
    }

    private void fillSearchItems(ArrayList<UItem> items) {
        if (searchWas) {
            for (int i = 0; i < searchResults.size(); i++) {
                items.add(SettingsSearchCell.Factory.of(resultNames.get(i), searchResults.get(i)));
            }
            if (!searchResults.isEmpty()) items.add(UItem.asShadow(null));
        }
    }

    private void search(String text) {
        lastSearchString = text;
        if (searchRunnable != null) {
            Utilities.searchQueue.cancelRunnable(searchRunnable);
            searchRunnable = null;
        }
        if (TextUtils.isEmpty(text)) {
            searchWas = false;
            searchResults.clear();
            resultNames.clear();
            listView.adapter.update(true);
            return;
        }
        Utilities.searchQueue.postRunnable(searchRunnable = () -> {
            var results = new ArrayList<SearchResult>();
            var names = new ArrayList<CharSequence>();
            var lowerQuery = text.toLowerCase();
            for (var result : searchArray) {
                var title = result.searchTitle.toLowerCase();
                var index = title.indexOf(lowerQuery);
                var matchLen = lowerQuery.length();
                if (index < 0) continue;
                var ssb = new SpannableStringBuilder(result.searchTitle);
                ssb.setSpan(new ForegroundColorSpan(getThemedColor(Theme.key_windowBackgroundWhiteBlueText4)), index, Math.min(index + matchLen, ssb.length()), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                results.add(result);
                names.add(ssb);
            }

            AndroidUtilities.runOnUIThread(() -> {
                if (!text.equals(lastSearchString)) {
                    return;
                }
                searchWas = true;
                searchResults.clear();
                resultNames.clear();
                searchResults.addAll(results);
                resultNames.addAll(names);
                listView.adapter.update(true);
            });
        }, 300);
    }
}
