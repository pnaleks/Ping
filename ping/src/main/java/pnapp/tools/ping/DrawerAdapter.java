/*
 *  Copyright (c) 2016 P.N.Alekseev <pnaleks@gmail.com>
 */
package pnapp.tools.ping;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

class DrawerAdapter extends BaseAdapter {
	private static final int[] LAYOUTS = {
		R.layout.drawer_group,
		R.layout.drawer_item,
		R.layout.drawer_child
	};
	static final int TYPE_GROUP = 0;
	static final int TYPE_ITEM  = 1;
	static final int TYPE_CHILD = 2;
	
	class Item {
		int mType;
        int mNameId = 0;
        int mIconId = 0;
		// int mItemId = 0;
        String mName;
        String mDescription;
		
		Item(int type, int nameResId) {
			mType   = type;
			mNameId = nameResId;
			mName   = null;
		}

		Item(int type, String name) {
			mType   = type;
			mNameId = 0;
			mName   = name;
		}
	}
	
	private ArrayList<Item> mItems = new ArrayList<>();

	@Override public boolean areAllItemsEnabled() { return false; }
	@Override public int getItemViewType(int position) { return mItems.get(position).mType; }
	@Override public int getViewTypeCount() { return LAYOUTS.length; }
	@Override public int getCount() { return mItems.size(); }
	@Override public boolean hasStableIds() { return false; }
	@Override public boolean isEmpty() { return mItems.isEmpty(); }
	@Override public boolean isEnabled(int position) { return mItems.get(position).mType > 0; }
	@Override public Object getItem(int position) { return mItems.get(position); }
	@Override public long getItemId(int position) { return 0; }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Item i = mItems.get(position);
		
		View v = convertView;
		if ( v == null ) v = inflater.inflate(LAYOUTS[i.mType], parent, false);
		
		TextView tv = (TextView) v.findViewById(android.R.id.text1); 
		if ( i.mName == null ) tv.setText(i.mNameId);
		else                   tv.setText(i.mName);
		
		if ( i.mIconId > 0 ) {
			ImageView iv = (ImageView) v.findViewById(android.R.id.icon);
			if (iv != null) iv.setImageResource(i.mIconId);
		}
		
		tv = (TextView) v.findViewById(android.R.id.text2);
		if (tv != null) {
			if ( i.mDescription == null ) tv.setVisibility(View.GONE);
			else tv.setText(i.mDescription);
		}
		return v;
	}
	
	void addGroup(int nameId, int iconId) {
		int i = 0;
		for ( Item item : mItems ) {
			if ( item.mType == TYPE_ITEM ) break;
			i++;
		}
		Item item = new Item(TYPE_GROUP, nameId);
		item.mIconId = iconId;
		mItems.add(i, item);
		notifyDataSetChanged();
	}

    boolean exists(int groupId) {
        for( Item item : mItems ) {
            if ( item.mType == TYPE_GROUP && item.mNameId == groupId ) return true;
        }
        return false;
    }

    /*
	public void addItem(String name, int id) {
		int i = 0;
		for( Item item : mItems ) {
			if ( item.mType != TYPE_ITEM ) { i++; continue; }
			if ( name.compareToIgnoreCase(item.mName) < 0 ) break;
			i++;
		}
		Item item = new Item(TYPE_ITEM, name);
		item.mItemId = id;
		mItems.add(i, item);
		notifyDataSetChanged();
	}
	*/
	
	boolean addChild(int groupId, String name) { return addChild(groupId, name, null); }
	boolean addChild(int groupId, String name, String description) {
		int i = 0;
		boolean inside = false;
		for( Item item : mItems ) {
			if ( inside ) {
                if (  item.mType == TYPE_CHILD && name.equalsIgnoreCase(item.mName) ) { // Do not duplicate items!!!
                    if ( description != null ) {
                        if (item.mDescription == null) {
                            item.mDescription = description;
                            notifyDataSetChanged();
                        } else if ( !item.mDescription.contains(description) ) {
                            item.mDescription += ", " + description;
                            notifyDataSetChanged();
                        }
                    }
                    return false;
                }
				if (  item.mType != TYPE_CHILD || name.compareToIgnoreCase(item.mName) < 0 ) break;
			} else {
				if ( item.mType == TYPE_GROUP && item.mNameId == groupId ) {
					inside = true;
				}
			}
			i++;
		}
        if ( inside ) {
            Item child = new Item(TYPE_CHILD, name);
            child.mDescription = description;
            mItems.add(i, child);
            notifyDataSetChanged();
            return true;
        }
        return false;
	}

	boolean removeChild(int groupId, String name) {
		int i = 0;
		boolean inside = false;
		for( Item item : mItems ) {
			if ( inside ) {
				if ( item.mType != TYPE_CHILD ) break;
				if ( name.equals(item.mName) ) {
					mItems.remove(i);
					notifyDataSetChanged();
					return true;
				}
			} else {
				if ( item.mType == TYPE_GROUP && item.mNameId == groupId ) {
					inside = true;
				}
			}
			i++;
		}
		return false;
	}

    boolean removeChildren(int groupId) {
        int i = 0;
        for( Item item : mItems ) {
            if ( item.mType == TYPE_GROUP && item.mNameId == groupId ) {
                i++;
                while( mItems.size() > i && mItems.get(i).mType == TYPE_CHILD ) {
                    mItems.remove(i);
                }
                notifyDataSetChanged();
                return true;
            }
            i++;
        }
        return false;
    }

    /*
    public int getChildrenCount(int groupId) {
        int i = 0;
        int count = 0;
        for( Item item : mItems ) {
            if ( item.mType == TYPE_GROUP && item.mNameId == groupId ) {
                while( mItems.size() > i && mItems.get(i).mType == TYPE_CHILD ) {
                    count++;
                    i++;
                }
                return count;
            }
            i++;
        }
        return 0;
    }
    */
}
