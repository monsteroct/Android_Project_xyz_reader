package com.example.xyzreader.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ShareCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = "ArticleDetailFragment";
    public static final String ARG_ITEM_ID = "item_id";
    public static final String BODY_TEXT_DELIMINATOR = "@DeLiMiNaTor";

    private Cursor mCursor;
    private long mItemId;

    private View mRootView;
    private ImageView mPhotoView;
    private TextView titleView;
    private TextView bylineView;
    private ProgressBar progressBar;
    private RecyclerView bodyRecyclerView;
    private BodyTextAdapter bodyTextAdapter;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // In support library r8, calling initLoader for a fragment in a FragmentPagerAdapter in
        // the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(0, null, this);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        // link thumbnail and header image to form a shared element transition
        mPhotoView.setTransitionName(getString(R.string.shared_element_thumbnail_name, mItemId));

        // set share fab function
        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText(getString(R.string.msg_share_fab))
                        .getIntent(), getString(R.string.action_share)));
            }
        });


        // init View references
        titleView = (TextView) mRootView.findViewById(R.id.article_title);
        bylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        bylineView.setMovementMethod(new LinkMovementMethod());
        bodyRecyclerView = (RecyclerView) mRootView.findViewById(R.id.rv_article_body);
        progressBar = (ProgressBar) mRootView.findViewById(R.id.pb_detail_loading);

        // setup RecyclerView
        bodyRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        bodyTextAdapter = new BodyTextAdapter(new ArrayList<Spannable>());
        bodyRecyclerView.setAdapter(bodyTextAdapter);

        bindViews();
        return mRootView;
    }


    private Date parsePublishedDate() {
        try {
            String date = mCursor.getString(ArticleLoader.Query.PUBLISHED_DATE);
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        if (mCursor != null) {
            Log.d(TAG, "start loading");
            //start loading
            showLoadingIndicator(true);

            // load image
            Picasso.get()
                    .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                    .error(R.drawable.ic_broken_image)
                    .into(mPhotoView, new Callback() {
                        @Override
                        public void onSuccess() {
                            showLoadingIndicator(false);
                            Log.d(TAG, "image loading done");
                            getActivity().startPostponedEnterTransition();
                        }

                        @Override
                        public void onError(Exception e) {
                            showLoadingIndicator(false);
                        }
                    });

            // load title
            titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));

            // load subtitle
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                bylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                bylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mCursor.getString(ArticleLoader.Query.AUTHOR)
                                + "</font>"));

            }

            // body text takes long time to load, -> load and process asynchronously to prevent ANR
            new textProcessAsyncTask().execute(mCursor.getString(ArticleLoader.Query.BODY));


        } else {
            Log.d(TAG, "empty cursor");
            showLoadingIndicator(true);
        }
    }

    private void showLoadingIndicator(boolean isLoading) {
        if (isLoading){
            mRootView.setAlpha(0.5f);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            mRootView.setAlpha(1);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(getActivity(), mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        if (!isAdded()) {
            if (cursor != null) {
                cursor.close();
            }
            return;
        }

        mCursor = cursor;
        if (mCursor != null && !mCursor.moveToFirst()) {
            Log.e(TAG, "Error reading item detail cursor");
            mCursor.close();
            mCursor = null;
            getLoaderManager().initLoader(0, null, this);
        }

        bindViews();
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
        bindViews();
    }

    class textProcessAsyncTask extends AsyncTask<String, Void, List<Spannable>>{

        @Override
        protected List<Spannable> doInBackground(String... strings) {
            CharSequence[] textPieces = strings[0]
                    .replaceAll("(\r\n\r\n|\n\n)", "<br />" + BODY_TEXT_DELIMINATOR)
                    .split(BODY_TEXT_DELIMINATOR);

            List<Spannable> spannableList = new ArrayList<>();
            for (CharSequence piece : textPieces){
                // CharSequence -> String -> resolve html tags by Html.fromHtml (return Spanned)
                // Spanned -> SpannableString (implementation of Spannable)
                spannableList.add(new SpannableString(Html.fromHtml(piece.toString())));
            }
            return spannableList;
        }

        @Override
        protected void onPostExecute(List<Spannable> spannables) {
            bodyTextAdapter.setSpannableList(spannables);
        }
    }

}
