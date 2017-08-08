package ca.igeneric.virusapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FilesListActivity extends Activity {

    private List<String> directoryStructure = new ArrayList<>();
    private File path;
    private ImageView imageView;
    private ListView listView;
    private RelativeLayout frame;
    private String fileSelected = "";
    private TextView imageTv, imageTv2;
    private boolean viewImage = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_files_list);
        setResult(RESULT_CANCELED);
        listView = findViewById(R.id.list);
        imageView = findViewById(R.id.imageView);
        frame = findViewById(R.id.frame);
        imageTv = findViewById(R.id.imageTv);
        imageTv2 = findViewById(R.id.imageTv2);
        Button back = findViewById(R.id.imageBack);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                viewImage = false;
                frame.setVisibility(View.GONE);
            }
        });
        Button select = findViewById(R.id.imageSelect);
        select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("FILE", fileSelected);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        directoryStructure.add("DCIM");
        directoryStructure.add("Camera");
        loadFilesList();
    }

    private void loadFilesList() {
        final ArrayList<String> filList = new ArrayList<>();
        ArrayList<String> dirList = new ArrayList<>();
        final Item[] list;
        path = getPath();
        try {
            path.mkdirs();
        } catch (SecurityException e) {
            Log.e("ERROR", "Unable to write on the sd card");
        }
        if (path.exists()) {
            String[] fullList = path.list();
            for (String file : fullList) {
                File sel = new File(path, file);
                if (!sel.isDirectory()) {
                    for (String extension : new String[]{".jpg", ".png", ".gif", ".jpeg", ".bmp"}) {
                        if (file.toLowerCase().endsWith(extension)) {
                            filList.add(file);
                            break;
                        }
                    }
                } else dirList.add(file);
            }
        }
        int len = filList.size() + dirList.size();
        int index = 0;
        if (directoryStructure.size() > 0) len++;
        list = new Item[len];
        if (directoryStructure.size() > 0) list[index++] = new Item("Up", R.drawable.directory_up);
        java.util.Collections.sort(filList);
        java.util.Collections.sort(dirList);
        for (String s : dirList) list[index++] = new Item(s, R.drawable.directory_icon);
        for (String s : filList) list[index++] = new Item(s, R.drawable.file_icon);

        ListAdapter adapter = new ArrayAdapter<Item>(this, R.layout.dialog_item, android.R.id.text1, list) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                textView.setCompoundDrawablesWithIntrinsicBounds(list[position].icon, 0, 0, 0);
                int dp5 = (int) (5 * getResources().getDisplayMetrics().density + 0.5f);
                textView.setCompoundDrawablePadding(dp5);
                return view;
            }
        };
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                String chosenFile = list[i].file;
                if (list[i].icon == R.drawable.directory_icon) setPath(list[i].file);
                else if (list[i].icon == R.drawable.directory_up) pathBack();
                else {
                    fileSelected = getPath() + "/" + list[i].file;
                    imageTv.setText(list[i].file);
                    imageTv2.setText(getPath()+"/");
                    imageView.setImageBitmap(MainActivity.getResizedBitmap(BitmapFactory.decodeFile(fileSelected),MainActivity.metrics.widthPixels,MainActivity.metrics.heightPixels));
                    frame.setVisibility(View.VISIBLE);
                    viewImage = true;
                }
            }
        });
    }

    private class Item {
        String file;
        int icon;
        Item(String file, Integer icon) {
            this.file = file;
            this.icon = icon;
        }
        @Override
        public String toString() {
            return file;
        }
    }

    public void setPath(String s) {
        directoryStructure.add(s);
        loadFilesList();
    }

    public void pathBack() {
        directoryStructure.remove(directoryStructure.size()-1);
        loadFilesList();
    }

    public File getPath() {
        String dir = "";
        if (directoryStructure.size() > 0) {
            for (String s : directoryStructure) {
                dir += "/";
                dir += s;
            }
        } else dir = "/";
        path = new File(Environment.getExternalStorageDirectory() + dir);
        return path;
    }

    @Override
    public void onBackPressed() {
        if (viewImage) {
            viewImage = false;
            frame.setVisibility(View.GONE);
        } else super.onBackPressed();
    }
}