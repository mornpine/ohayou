package com.ohayou.japanese.model;

import com.ohayou.japanese.utils.CommUtils;
import com.ohayou.japanese.utils.Constants;

import java.io.File;

/**
 * Created by Oxygen on 15/9/26.
 */
public class Textbook {
    public int tid;
    public int cid;
    public int passed;
    public int failed;
    public int complete;
    public String questions;
    public String name;
    public String level;
    public String status;

    public int getStatus() {
        File file = new File(getTextbookZipPath());
        if (file.exists()) {
            if (CommUtils.unZip(file, Constants.TEXTBOOK_PATH)) {
                file.delete();
                return TEXTBOOK_STATUS_READY;
            } else {
                file.delete();
                return TEXTBOOK_STATUS_NEW;
            }
        }

        file = getTextbookDir();
        if (file.exists()) {
            if (file.isDirectory()) {
                if (passed > 0 || failed > 0) {
                    if (passed + failed == status.length()) {
                        return TEXTBOOK_STATUS_FINISH;
                    }
                    return TEXTBOOK_STATUS_ABORT;
                }
                return TEXTBOOK_STATUS_READY;
            } else {
                file.delete();
            }
        }

        return TEXTBOOK_STATUS_NEW;
    }

    public String getTextbookZipPath() {
        return Constants.TEXTBOOK_PATH + "/" + tid + ".zip";
    }

    public String getTextbookSize() {
        return CommUtils.formatSize(CommUtils.getFolderSize(getTextbookDir()));
    }

    public File getTextbookDir() {
        return new File(Constants.TEXTBOOK_PATH + "/" + tid);
    }

    public static final int TEXTBOOK_STATUS_NEW = 1;
    public static final int TEXTBOOK_STATUS_DOWNLOADING = 2;
    public static final int TEXTBOOK_STATUS_READY = 3;
    public static final int TEXTBOOK_STATUS_ABORT = 4;
    public static final int TEXTBOOK_STATUS_FINISH = 5;
}
