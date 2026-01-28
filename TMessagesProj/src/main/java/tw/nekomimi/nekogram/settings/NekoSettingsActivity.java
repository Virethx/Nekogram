package tw.nekomimi.nekogram.settings;

import android.content.Context;
import android.view.View;
import android.view.accessibility.AccessibilityManager;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.LaunchActivity;

import java.util.ArrayList;
import java.util.List;

import tw.nekomimi.nekogram.accessibility.AccessibilitySettingsActivity;
import tw.nekomimi.nekogram.helpers.CloudSettingsHelper;
import tw.nekomimi.nekogram.helpers.PasscodeHelper;
import tw.nekomimi.nekogram.helpers.remote.ConfigHelper;
import tw.nekomimi.nekogram.helpers.remote.UpdateHelper;

public class NekoSettingsActivity extends BaseNekoSettingsActivity {

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

    @Override
    public View createView(Context context) {
        View fragmentView = super.createView(context);

        actionBar.createMenu()
                .addItem(0, R.drawable.cloud_sync)
                .setOnClickListener(v -> CloudSettingsHelper.getInstance().showDialog(this));

        return fragmentView;
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
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
}
