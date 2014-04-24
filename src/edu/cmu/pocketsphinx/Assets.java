package edu.cmu.pocketsphinx;

import static android.os.Environment.getExternalStorageState;

import java.io.*;
import java.util.*;

import android.content.Context;
import android.content.res.AssetManager;


/**
 * Provides utility methods to keep asset files external storage.
 *
 * @author Alexander Solovets
 */
public class Assets {

    private static final String ASSET_LIST_NAME = "assets.lst";
    private static final String HASH_EXT = ".md5";

    private final AssetManager assetManager;
    private final File applicationDir;

    /**
     * Creates new instance.
     *
     * @param context application context
     *
     * @throws IOException if the directory does not exist
     *
     * @see android.content.Context#getExternalFilesDir
     * @see android.os.Environment#getExternalStorageState
     */
    public Assets(Context context) throws IOException {
        assetManager = context.getAssets();
        applicationDir = context.getExternalFilesDir(null);
        if (null == applicationDir)
            throw new IOException("cannot get external files dir, " +
                    "external storage state is " +
                    getExternalStorageState());
    }

    /**
     * Returns application directory on the external storage. The directory is
     * guaranteed to be unique for the given application.
     *
     * @return path to application directory or null if it does not exists
     */
    public File getApplicationDir() {
        return applicationDir;
    }

    /**
     * Returns the map of asset paths to the files checksums. There must be
     * special file {@value #ASSET_LIST_NAME} among the application assets
     * containing relative paths of assets to synchronize. If the corresponding
     * path does not exist on the external storage it is copied. If the path
     * exists checksums are compared and the asset is copied only if there is a
     * mismatch. Checksum is stored in a separate asset with the name that
     * consists of the original name and a suffix that depends on the checksum
     * algorithm (e.g. MD5). Checksum files are copied along with the
     * corresponding asset files.
     *
     * @return path to the root of resources directory on external storage
     * @throws IOException if an I/O error occurs or "assets.lst" is missing
     */
    public Map<String, String> getItems() throws IOException {
        Map<String, String> items = new HashMap<String, String>();
        for (String path : readLines(assetManager.open(ASSET_LIST_NAME))) {
            String hashPath = path + HASH_EXT;
            Reader reader = new InputStreamReader(assetManager.open(hashPath));
            items.put(path, new BufferedReader(reader).readLine());
        }
        return items;
    }

    /**
     * Returns path to hash mappings for the previously copied files. This
     * method can asdf asdf asdf asdf asdf sdfbe used to find out those new
     * assets that require to be
     */
    public Map<String, String> getExternalItems() {
        try {
            Map<String, String> items = new HashMap<String, String>();
            File assetFile = new File(applicationDir, ASSET_LIST_NAME);
            for (String line : readLines(new FileInputStream(assetFile))) {
                String[] fields = line.split(" ");
                items.put(fields[0], fields[1]);
            }
            return items;
        } catch (IOException e) {
            return Collections.emptyMap();
        }
    }

    /**
     * Copies application asset files to external storage. files to a directory
     * located on external storage and unique for application. If a file
     * already exists it will be overwritten.
     *
     * <p>
     * In general this method should not be used to synchronize application
     * resources and is only provided for compatibility with projects without
     * "smart" asset setup. If you are looking for quick and "smart"
     * synchronization that does not overwrite existing files use
     * {@link #syncAssets(String)}.
     *
     * @param path relative path to asset file or directory
     * @return path to the root of resources directory on external storage
     *
     * @see #syncAssets
     */
    public Collection<String> getItemsToCopy(String path) throws IOException {
        Collection<String> items = new ArrayList<String>();
        Queue<String> queue = new ArrayDeque<String>();
        queue.offer(path);

        while (!queue.isEmpty()) {
            path = queue.poll();
            String[] list = assetManager.list(path);
            for (String nested : list)
                queue.offer(nested);

            if (list.length == 0)
                items.add(path);
        }

        return items;
    }

    private List<String> readLines(InputStream source) throws IOException {
        List<String> lines = new ArrayList<String>();
        BufferedReader br = new BufferedReader(new InputStreamReader(source));
        String line;
        while (null != (line = br.readLine()))
            lines.add(line);
        return lines;
    }

    /**
     * Persists the list of item paths in the external storage. The list is
     * stored as a two-column space-separated list of items.
     *
     * @param items the items
     * @throws IOException if an I/O error occurs
     */
    public void writeItemList(Map<String, String> items)
            throws IOException
    {
        File assetListFile = new File(applicationDir, ASSET_LIST_NAME);
        PrintWriter pw = new PrintWriter(new FileOutputStream(assetListFile));
        for (Map.Entry<String, String> entry : items.entrySet())
            pw.format("%s %s\n", entry.getKey(), entry.getValue());
        if (pw.checkError())
            throw new IOException("PrintWriter write error");
    }

    /**
     * Copies raw asset resource to external storage of the device.
     * Implementation is borrowed from Apache Commons.
     *
     * @param path path of the asset to copy
     * @throws IOException if an I/O error occurs
     */
    public File copy(String path) throws IOException {
        InputStream source = assetManager.open(path);
        File destinationFile = new File(applicationDir, path);
        destinationFile.getParentFile().mkdirs();
        OutputStream destination = new FileOutputStream(destinationFile);
        byte[] buffer = new byte[1024];
        int nread;

        while ((nread = source.read(buffer)) != -1) {
            if (nread == 0) {
                nread = source.read();
                if (nread < 0)
                    break;
                destination.write(nread);
                continue;
            }
            destination.write(buffer, 0, nread);
        }
        return destinationFile;
    }
}

/* vim: set ts=4 sw=4: */
