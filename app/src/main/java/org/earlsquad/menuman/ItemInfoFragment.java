package org.earlsquad.menuman;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


public class ItemInfoFragment extends Fragment {


  public ItemInfoFragment() {
    // Required empty public constructor
  }


  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    // Inflate the layout for this fragment
    View view = inflater.inflate(R.layout.fragment_item_info, container, false);
    Bundle args = getArguments();
    String name = args.getString("MenuItemName");
    MenuDatabase database = new MenuDatabase(getContext());
    MenuItem result = database.search(name);

    TextView foreignName = view.findViewById(R.id.menu_item_foreign_name);
    TextView englishName = view.findViewById(R.id.menu_item_english_name);

    foreignName.setText(result.getRealName());
    englishName.setText(result.getEngName());
    return view;
  }

}
