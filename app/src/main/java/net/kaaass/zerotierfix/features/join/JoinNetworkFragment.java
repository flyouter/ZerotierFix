package net.kaaass.zerotierfix.features.join;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.google.android.material.tabs.TabLayout;

import net.kaaass.zerotierfix.ZerotierFixApplication;
import net.kaaass.zerotierfix.R;
import net.kaaass.zerotierfix.data.repository.NetworkRepository;
import net.kaaass.zerotierfix.model.dto.JoinNetworkRequest;
import net.kaaass.zerotierfix.model.enums.DNSMode;
import net.kaaass.zerotierfix.util.NetworkIdUtils;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// TODO: clear up
public class JoinNetworkFragment extends Fragment implements CustomDNSListener {
    public static final String TAG = "JoinNetwork";
    EventBus eventBus = EventBus.getDefault();
    private int mDNSMode = 0;
    private TabLayout mDNSTabLayout;
    private ViewPager mDNSViewSwitcher;
    private String mDNSv4_1 = "";
    private String mDNSv4_2 = "";
    private String mDNSv6_1 = "";
    private String mDNSv6_2 = "";
    private CheckBox mDefaultRouteCheckBox;
    private Button mJoinButton;
    private EditText mNetworkIdTextView;

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
    }

    @Override // androidx.fragment.app.Fragment
    public void onActivityCreated(Bundle bundle) {
        super.onActivityCreated(bundle);
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater layoutInflater, ViewGroup viewGroup, Bundle bundle) {
        super.onCreateView(layoutInflater, viewGroup, bundle);
        View inflate = layoutInflater.inflate(R.layout.fragment_join_network, viewGroup, false);
        CheckBox checkBox = inflate.findViewById(R.id.join_network_default_route);
        this.mDefaultRouteCheckBox = checkBox;
        checkBox.setEnabled(false);
        this.mDNSTabLayout = inflate.findViewById(R.id.dns_tab_layout);
        this.mDNSViewSwitcher = inflate.findViewById(R.id.dns_view_pager);
        this.mDNSViewSwitcher.setAdapter(new DNSPagerAdapter(inflate.getContext(), getParentFragmentManager(), this));
        this.mDNSTabLayout.setupWithViewPager(this.mDNSViewSwitcher);
        this.mDNSTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            /* class com.zerotier.one.ui.JoinNetworkFragment.AnonymousClass1 */

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabReselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override // com.google.android.material.tabs.TabLayout.BaseOnTabSelectedListener
            public void onTabSelected(TabLayout.Tab tab) {
                Log.d(JoinNetworkFragment.TAG, "Tab " + tab.getPosition() + " Selected");
                JoinNetworkFragment.this.mDNSMode = tab.getPosition();
            }
        });
        EditText editText = inflate.findViewById(R.id.join_network_edit_text);
        this.mNetworkIdTextView = editText;
        editText.addTextChangedListener(new TextValidator(this.mNetworkIdTextView) {
            /* class com.zerotier.one.ui.JoinNetworkFragment.AnonymousClass2 */

            @Override // com.zerotier.one.ui.JoinNetworkFragment.TextValidator
            public void validate(EditText editText, String str) {
                if (str.length() == 16) {
                    JoinNetworkFragment.this.mJoinButton.setEnabled(true);
                    JoinNetworkFragment.this.mDefaultRouteCheckBox.setEnabled(true);
                    return;
                }
                JoinNetworkFragment.this.mJoinButton.setEnabled(false);
                JoinNetworkFragment.this.mDefaultRouteCheckBox.setChecked(false);
                JoinNetworkFragment.this.mDefaultRouteCheckBox.setEnabled(false);
            }
        });
        this.mDefaultRouteCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            /* class com.zerotier.one.ui.JoinNetworkFragment.AnonymousClass3 */

            public void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                if (JoinNetworkFragment.this.mDefaultRouteCheckBox.isEnabled()) {
                    String obj = JoinNetworkFragment.this.mNetworkIdTextView.getText().toString();
                    SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(JoinNetworkFragment.this.getActivity());
                    SharedPreferences.Editor edit = defaultSharedPreferences.edit();
                    Set<String> stringSet = defaultSharedPreferences.getStringSet("default_route_enabled_networks", null);
                    if (z) {
                        if (stringSet == null) {
                            HashSet hashSet = new HashSet();
                            hashSet.add(obj);
                            edit.putStringSet("default_route_enabled_networks", hashSet);
                        } else if (!stringSet.contains(obj)) {
                            HashSet hashSet2 = new HashSet(stringSet);
                            hashSet2.add(obj);
                            edit.putStringSet("default_route_enabled_networks", hashSet2);
                        }
                    } else if (stringSet != null && stringSet.contains(obj)) {
                        HashSet hashSet3 = new HashSet(stringSet);
                        hashSet3.remove(obj);
                        edit.putStringSet("default_route_enabled_networks", hashSet3);
                    }
                    edit.apply();
                }
            }
        });
        Button button = inflate.findViewById(R.id.button_join_network);
        this.mJoinButton = button;
        button.setEnabled(false);
        this.mJoinButton.setOnClickListener(new View.OnClickListener() {
            /* class com.zerotier.one.ui.JoinNetworkFragment.AnonymousClass4 */

            public void onClick(View view) {
                try {
                    String networkIdStr = JoinNetworkFragment.this.mNetworkIdTextView.getText().toString();
                    long networkId = NetworkIdUtils.hexStringToLong(networkIdStr);
                    boolean routeViaZeroTier = JoinNetworkFragment.this.mDefaultRouteCheckBox.isChecked();
                    List<String> dnsServers = new ArrayList<>();
                    if (JoinNetworkFragment.this.mDNSMode == DNSMode.CUSTOM_DNS.toInt()) {
                        addDnsServer(dnsServers, JoinNetworkFragment.this.mDNSv4_1);
                        addDnsServer(dnsServers, JoinNetworkFragment.this.mDNSv4_2);
                        addDnsServer(dnsServers, JoinNetworkFragment.this.mDNSv6_1);
                        addDnsServer(dnsServers, JoinNetworkFragment.this.mDNSv6_2);
                    }
                    NetworkRepository repository = ((ZerotierFixApplication)
                            JoinNetworkFragment.this.requireActivity().getApplication())
                            .getAppContainer()
                            .getNetworkRepository();
                    JoinNetworkRequest request = new JoinNetworkRequest(
                            networkId,
                            networkIdStr,
                            routeViaZeroTier,
                            JoinNetworkFragment.this.mDNSMode,
                            dnsServers
                    );
                    if (!repository.joinNetwork(request)) {
                        Log.e(JoinNetworkFragment.TAG, "Network already present");
                    } else {
                        Log.d(JoinNetworkFragment.TAG, "Joining network " + networkIdStr);
                    }
                    JoinNetworkFragment.this.requireActivity().onBackPressed();
                } catch (Throwable th) {
                    JoinNetworkFragment.this.requireActivity().onBackPressed();
                    throw th;
                }
            }
        });
        return inflate;
    }

    /**
     * 将合法的 DNS 地址加入列表。
     *
     * @param servers 目标地址列表
     * @param server  DNS 地址；为空或格式非法时忽略
     */
    private static void addDnsServer(List<String> servers, String server) {
        if (!server.isEmpty() && InetAddressValidator.getInstance().isValid(server)) {
            servers.add(server);
        }
    }

    @Override // com.zerotier.one.ui.CustomDNSListener
    public void setDNSv4_1(String str) {
        this.mDNSv4_1 = str;
    }

    @Override // com.zerotier.one.ui.CustomDNSListener
    public void setDNSv4_2(String str) {
        this.mDNSv4_2 = str;
    }

    @Override // com.zerotier.one.ui.CustomDNSListener
    public void setDNSv6_1(String str) {
        this.mDNSv6_1 = str;
    }

    @Override // com.zerotier.one.ui.CustomDNSListener
    public void setDNSv6_2(String str) {
        this.mDNSv6_2 = str;
    }

    abstract class TextValidator implements TextWatcher {
        private final EditText editText;

        public TextValidator(EditText editText2) {
            this.editText = editText2;
        }

        public final void beforeTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public final void onTextChanged(CharSequence charSequence, int i, int i2, int i3) {
        }

        public abstract void validate(EditText editText2, String str);

        public final void afterTextChanged(Editable editable) {
            validate(this.editText, this.editText.getText().toString());
        }
    }
}
