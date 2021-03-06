/*   This file is part of My Expenses.
 *   My Expenses is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   My Expenses is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with My Expenses.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.totschnig.myexpenses.fragment;

import static org.totschnig.myexpenses.provider.DatabaseConstants.*;

import org.totschnig.myexpenses.R;
import org.totschnig.myexpenses.activity.ExpenseEdit;
import org.totschnig.myexpenses.activity.ManageTemplates;
import org.totschnig.myexpenses.model.Transaction;
import org.totschnig.myexpenses.provider.DbUtils;
import org.totschnig.myexpenses.provider.TransactionProvider;
import org.totschnig.myexpenses.util.Utils;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class TemplatesList extends BudgetListFragment implements LoaderManager.LoaderCallbacks<Cursor> {
  public static final int TEMPLATES_CURSOR=1;
  Cursor mTemplatesCursor;
  private SimpleCursorAdapter mAdapter;
  //private SimpleCursorAdapter mAdapter;
  //private StickyListHeadersListView mListView;
  int mGroupIdColumnIndex;
  private LoaderManager mManager;
  
  private int columnIndexAmount, columnIndexLabelSub, columnIndexComment,
    columnIndexPayee, columnIndexColor,columnIndexTransferPeer,
    columnIndexCurrency;
  boolean indexesCalculated = false;
  
  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    setColors();
    View v = inflater.inflate(R.layout.templates_list, null, false);
    ListView lv = (ListView) v.findViewById(R.id.list);

    mManager = getLoaderManager();
    mManager.initLoader(TEMPLATES_CURSOR, null, this);
    // Create an array to specify the fields we want to display in the list
    String[] from = new String[]{KEY_TITLE,KEY_LABEL_MAIN,KEY_AMOUNT};
    // and an array of the fields we want to bind those fields to 
    int[] to = new int[]{R.id.title,R.id.category,R.id.amount};
    mAdapter = new MyAdapter(
        getActivity(), 
        R.layout.template_row,
        null,
        from,
        to,
        0);
    lv.setAdapter(mAdapter);
    lv.setEmptyView(v.findViewById(R.id.empty));
    registerForContextMenu(lv);
    return v;
  }
  @Override
  public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
    super.onCreateContextMenu(menu, v, menuInfo);
    menu.add(0,R.id.CREATE_INSTANCE_SAVE_COMMAND,0,R.string.menu_apply_template_and_save);
    menu.add(0,R.id.CREATE_INSTANCE_EDIT_COMMAND,0,R.string.menu_apply_template_and_edit);
  }
  @Override
  public boolean onContextItemSelected(MenuItem item) {
    if (!getUserVisibleHint())
      return false;
    AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
    if (((ManageTemplates) getActivity()).dispatchCommand(item.getItemId(),info.id))
      return true;
    switch(item.getItemId()) {
    case R.id.CREATE_INSTANCE_EDIT_COMMAND:
      Intent intent = new Intent(getActivity(), ExpenseEdit.class);
      intent.putExtra("template_id", info.id);
      intent.putExtra("instance_id", -1L);
      startActivity(intent);
      return true;
    case R.id.CREATE_INSTANCE_SAVE_COMMAND:
      getActivity().getSupportFragmentManager().beginTransaction()
        .add(TaskExecutionFragment.newInstance(TaskExecutionFragment.TASK_NEW_FROM_TEMPLATE,info.id, null), "ASYNC_TASK")
        .commit();
      return true;
      }
    return false;
  }
  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
    switch(id) {
    case TEMPLATES_CURSOR:
      return new CursorLoader(getActivity(),
        TransactionProvider.TEMPLATES_URI,
        null,
        KEY_PLANID + " is null",
        null,
        null);
    }
    return null;
  }
  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
    switch (loader.getId()) {
    case TEMPLATES_CURSOR:
      mTemplatesCursor = c;
      if (!indexesCalculated) {
        columnIndexAmount = c.getColumnIndex(KEY_AMOUNT);
        columnIndexLabelSub = c.getColumnIndex(KEY_LABEL_SUB);
        columnIndexComment = c.getColumnIndex(KEY_COMMENT);
        columnIndexPayee = c.getColumnIndex(KEY_PAYEE_NAME);
        columnIndexColor = c.getColumnIndex(KEY_COLOR);
        columnIndexTransferPeer = c.getColumnIndex(KEY_TRANSFER_PEER);
        columnIndexCurrency = c.getColumnIndex(KEY_CURRENCY);
        indexesCalculated = true;
      }
      ((SimpleCursorAdapter) mAdapter).swapCursor(mTemplatesCursor);
      break;
    }
  }
  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
      ((SimpleCursorAdapter) mAdapter).swapCursor(null);
  }
  public class MyAdapter extends SimpleCursorAdapter {
    String categorySeparator = " : ",
        commentSeparator = " / ";
    public MyAdapter(Context context, int layout, Cursor c, String[] from,
        int[] to, int flags) {
      super(context, layout, c, from, to, flags);
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      convertView=super.getView(position, convertView, parent);
      Cursor c = getCursor();
      c.moveToPosition(position);
      TextView tv1 = (TextView)convertView.findViewById(R.id.amount);
      long amount = c.getLong(columnIndexAmount);
      if (amount < 0) {
        tv1.setTextColor(colorExpense);
        // Set the background color of the text.
      }
      else {
        tv1.setTextColor(colorIncome);
      }
      tv1.setText(Utils.convAmount(amount,Utils.getSaveInstance(c.getString(columnIndexCurrency))));
      int color = c.getInt(columnIndexColor);
      convertView.findViewById(R.id.colorAccount).setBackgroundColor(color);
      TextView tv2 = (TextView)convertView.findViewById(R.id.category);
      CharSequence catText = tv2.getText();
      if (c.getInt(columnIndexTransferPeer) > 0) {
        catText = ((amount < 0) ? "=> " : "<= ") + catText;
      } else {
        Long catId = DbUtils.getLongOrNull(c,KEY_CATID);
        if (catId == null) {
          catText = getString(R.string.no_category_assigned);
        } else {
          String label_sub = c.getString(columnIndexLabelSub);
          if (label_sub != null && label_sub.length() > 0) {
            catText = catText + categorySeparator + label_sub;
          }
        }
      }
      SpannableStringBuilder ssb;
      String comment = c.getString(columnIndexComment);
      if (comment != null && comment.length() > 0) {
        ssb = new SpannableStringBuilder(comment);
        ssb.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC), 0, comment.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      String payee = c.getString(columnIndexPayee);
      if (payee != null && payee.length() > 0) {
        ssb = new SpannableStringBuilder(payee);
        ssb.setSpan(new UnderlineSpan(), 0, payee.length(), 0);
        catText = TextUtils.concat(catText,commentSeparator,ssb);
      }
      tv2.setText(catText);
      return convertView;
    }
  }
}
