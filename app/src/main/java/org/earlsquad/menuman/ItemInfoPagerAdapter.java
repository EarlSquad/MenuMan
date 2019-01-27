package org.earlsquad.menuman;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

public class ItemInfoPagerAdapter extends FragmentStatePagerAdapter {
  private final List<String> items;

  public ItemInfoPagerAdapter(FragmentManager fm, List<String> items) {
    super(fm);
    this.items = items;
  }

  @Override
  public Fragment getItem(int i) {
    Fragment fragment = new ItemInfoFragment();
    Bundle args = new Bundle();
    args.putString("MenuItemName", items.get(i));
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public int getCount() {
    return items.size();
  }
}
