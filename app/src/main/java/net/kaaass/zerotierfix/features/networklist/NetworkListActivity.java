package net.kaaass.zerotierfix.features.networklist;

import androidx.fragment.app.Fragment;

import net.kaaass.zerotierfix.ui.common.SingleFragmentActivity;

/**
 * 网络列表 fragment 的容器 activity
 */
public class NetworkListActivity extends SingleFragmentActivity {
    @Override
    public Fragment createFragment() {
        return new NetworkListFragment();
    }
}
