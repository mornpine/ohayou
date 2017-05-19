package com.ohayou.japanese.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import com.ohayou.japanese.OhayouApplication;
import com.ohayou.japanese.R;
import com.ohayou.japanese.adapter.CachedTextbookAdapter;
import com.ohayou.japanese.db.DatabaseHelper;
import com.ohayou.japanese.model.Textbook;
import com.ohayou.japanese.utils.Constants;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by Oxygen on 15/8/9.
 */
public class DeleteTextbookActivity extends BaseActivity implements View.OnClickListener {

    ListView mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_delete_textbook);

        mList = (ListView) findViewById(R.id.list);

        findViewById(R.id.back).setOnClickListener(this);

        mList.setEmptyView(findViewById(R.id.empty));
        initList();
    }

    void initList() {
        CachedTextbookAdapter adapter = new CachedTextbookAdapter(this);
        mList.setAdapter(adapter);

        DatabaseHelper helper = ((OhayouApplication) getApplication()).getDatabaseHelper();
        ArrayList<Textbook> data = new ArrayList<>();
        File textbookDir = new File(Constants.TEXTBOOK_PATH);
        if (textbookDir.isDirectory()) {
            for (String name : textbookDir.list()) {
                if (!name.startsWith(".")) {
                    try {
                        int tid = Integer.parseInt(name);
                        Textbook textbook = helper.getTextbook(tid);
                        if (textbook != null) {
                            data.add(textbook);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        adapter.setData(data);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
        }
    }
}
