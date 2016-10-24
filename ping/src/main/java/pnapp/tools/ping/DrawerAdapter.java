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
	enum ITEM_TYPE {
        HEADER(R.layout.drawer_header),
        ITEM(R.layout.drawer_item),
        GROUP(R.layout.drawer_group),
        CHILD(R.layout.drawer_child);

        private int layoutResId;

        ITEM_TYPE(int layoutResId) { this.layoutResId = layoutResId; }
    }
	
	class Item {
		ITEM_TYPE type;
        int id = 0;
        int iconId = 0;
        String name;
        String description;

        Item(ITEM_TYPE type) { this.type = type; }

        void bind(View view) {
            TextView nameView = (TextView) view.findViewById(android.R.id.text1);
            if (nameView != null) {
                if (iconId != 0) {
                    ImageView imageView = (ImageView) view.findViewById(android.R.id.icon);
                    if (imageView == null)
                        nameView.setCompoundDrawablesWithIntrinsicBounds(iconId, 0, 0, 0);
                    else
                        imageView.setImageResource(iconId);
                }

                if (name == null && id > 0) name = view.getResources().getString(id);
                if (name != null) nameView.setText(name);

                TextView descriptionView = (TextView) view.findViewById(android.R.id.text2);
                if (descriptionView != null) {
                    if (description == null) {
                        descriptionView.setVisibility(View.GONE);
                    } else {
                        descriptionView.setText(description);
                        descriptionView.setVisibility(View.VISIBLE);
                    }
                }
            }
        }

	}
	
	private ArrayList<Item> mItems = new ArrayList<>();

	@Override public boolean areAllItemsEnabled() { return false; }
	@Override public int getItemViewType(int position) { return mItems.get(position).type.ordinal(); }
	@Override public int getViewTypeCount() { return ITEM_TYPE.values().length; }
	@Override public int getCount() { return mItems.size(); }
	@Override public boolean hasStableIds() { return false; }
	@Override public boolean isEmpty() { return mItems.isEmpty(); }
	@Override public boolean isEnabled(int position) { return mItems.get(position).type != ITEM_TYPE.GROUP; }
	@Override public Object getItem(int position) { return mItems.get(position); }
	@Override public long getItemId(int position) { return 0; }

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		LayoutInflater inflater = (LayoutInflater) parent.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		Item item = mItems.get(position);
		
		View view = convertView;
		if ( view == null ) view = inflater.inflate(item.type.layoutResId, parent, false);

        item.bind(view);

		return view;
	}
	
	void addGroup(int nameId, int iconId) {
		int i = 0;
		for ( Item item : mItems ) {
			if ( item.type == ITEM_TYPE.ITEM ) break;
			i++;
		}
		Item item = new Item(ITEM_TYPE.GROUP);
        item.id = nameId;
		item.iconId = iconId;
		mItems.add(i, item);
		notifyDataSetChanged();
	}

    boolean exists(int groupId) {
        for( Item item : mItems ) {
            if ( item.type == ITEM_TYPE.GROUP && item.id == groupId ) return true;
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
                if (  item.type == ITEM_TYPE.CHILD && name.equalsIgnoreCase(item.name) ) { // Do not duplicate items!!!
                    if ( description != null ) {
                        if (item.description == null) {
                            item.description = description;
                            notifyDataSetChanged();
                        } else if ( !item.description.contains(description) ) {
                            item.description += ", " + description;
                            notifyDataSetChanged();
                        }
                    }
                    return false;
                }
				if (  item.type != ITEM_TYPE.CHILD || name.compareToIgnoreCase(item.name) < 0 ) break;
			} else {
				if ( item.type == ITEM_TYPE.GROUP && item.id == groupId ) {
					inside = true;
				}
			}
			i++;
		}
        if ( inside ) {
            Item child = new Item(ITEM_TYPE.CHILD);
            child.name = name;
            child.description = description;
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
				if ( item.type != ITEM_TYPE.CHILD ) break;
				if ( name.equals(item.name) ) {
					mItems.remove(i);
					notifyDataSetChanged();
					return true;
				}
			} else {
				if ( item.type == ITEM_TYPE.GROUP && item.id == groupId ) {
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
            if ( item.type == ITEM_TYPE.GROUP && item.id == groupId ) {
                i++;
                while( mItems.size() > i && mItems.get(i).type == ITEM_TYPE.CHILD ) {
                    mItems.remove(i);
                }
                notifyDataSetChanged();
                return true;
            }
            i++;
        }
        return false;
    }

    void setHeader(String title) {
        Item header = (mItems.size() > 0) ? mItems.get(0) : null;
        if (header == null || header.type != ITEM_TYPE.HEADER) {
            header =  new Item(ITEM_TYPE.HEADER);
            header.id = R.string.app_description;
            mItems.add(0, header);
        }

        header.name = title;
        notifyDataSetChanged();
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
