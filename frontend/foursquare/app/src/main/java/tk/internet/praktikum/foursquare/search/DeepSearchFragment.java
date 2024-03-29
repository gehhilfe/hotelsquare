package tk.internet.praktikum.foursquare.search;

//import android.app.Fragment;

import android.app.ProgressDialog;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.CursorAdapter;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.widget.SearchView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import tk.internet.praktikum.foursquare.MainActivity;
import tk.internet.praktikum.foursquare.R;
import tk.internet.praktikum.foursquare.api.ServiceFactory;
import tk.internet.praktikum.foursquare.api.bean.Location;
import tk.internet.praktikum.foursquare.api.bean.Prediction;
import tk.internet.praktikum.foursquare.api.bean.Venue;
import tk.internet.praktikum.foursquare.api.bean.VenueSearchQuery;
import tk.internet.praktikum.foursquare.api.service.PlaceService;
import tk.internet.praktikum.foursquare.api.service.VenueService;
import tk.internet.praktikum.foursquare.history.DaoSession;
import tk.internet.praktikum.foursquare.storage.LocalDataBaseManager;


public class DeepSearchFragment extends Fragment implements android.support.v7.widget.SearchView.OnQueryTextListener, PlaceSelectionListener {
    private final String URL = "https://dev.ip.stimi.ovh/";
    private final String GOOGLE_PLACE_URL = "https://maps.googleapis.com";
    private final String LOG = DeepSearchFragment.class.getSimpleName();
    private SearchView searchView;
    //private MaterialSearchView searchView;
    private VenueStatePageAdapter venueStatePageAdapter;
    private AutoCompleteTextView filterLocation;
    private ToggleButton mapViewButton;
    private SeekBar filterRadius;
    private TextView seekBarView;
    private List<?> optionalFilters;
    private List<Venue> venues;
    private View view;
    private boolean isNearMe;
    private boolean isMapView;
    private VenueViewPager venuesViewPager;
    private VenuesListFragment venuesListFragment = null;
    private String keyword;
    private int currentPage;
    private PlaceAdapter placeAdapter;
    private String lastFilterLocation;
    private ProgressDialog progressDialog;
    private String lastQuery;
    private boolean isChangedSearchText = false;
    private boolean submitNewQuery;
    private boolean reachedMaxVenues;
    private LinearLayout priceLinearLayout;
    private ToggleButton openNow_button;
    private ToggleButton price_1,price_2,price_3,price_4,price_5;
    private List<ToggleButton> prices;
    private  int price;
    private int lastPrice;
    private  boolean isQueryFromFastSearch=false;
    private boolean lastOpenNow;
    private Drawable selected_prices_background,unselected_prices_background,selected_money,unselected_money;
    private SimpleCursorAdapter keyWordsSuggestionAdapter;
    private final String KEY_WORD="suggestedWord";
    public DeepSearchFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        view = inflater.inflate(R.layout.fragment_deep_search, container, false);

        keyWordsSuggestionAdapter = new SimpleCursorAdapter(
                getActivity(),
                R.layout.key_words_list,
                null,
                new String[]{KEY_WORD},
                new int[]{R.id.suggested_keyword},
                0);

        venuesViewPager = (VenueViewPager) view.findViewById(R.id.venues_result);
        filterLocation = (AutoCompleteTextView) view.findViewById(R.id.location);

        filterRadius = (SeekBar) view.findViewById(R.id.seekBarRadius);
        filterRadius.setMax(50);
        seekBarView = (TextView) view.findViewById(R.id.seekBarView);

        mapViewButton = (ToggleButton) view.findViewById(R.id.is_map_view);
        mapViewButton.setText(null);
        mapViewButton.setTextOff(null);
        mapViewButton.setTextOn(null);
        isMapView = false;
        mapViewButton.setChecked(true);
        priceLinearLayout=(LinearLayout) view.findViewById(R.id.price_optional_filter) ;
        price_1=(ToggleButton) view.findViewById(R.id.price_1);
        price_2=(ToggleButton) view.findViewById(R.id.price_2);
        price_3=(ToggleButton) view.findViewById(R.id.price_3);
        price_4=(ToggleButton) view.findViewById(R.id.price_4);
        price_5=(ToggleButton) view.findViewById(R.id.price_5);
        prices=new ArrayList<>();
        prices.add(price_1);
        prices.add(price_2);
        prices.add(price_3);
        prices.add(price_4);
        prices.add(price_5);
        openNow_button=(ToggleButton)view.findViewById(R.id.open_now_optional_filter);
        setDefaultPriceToggleButton();
        getDrawable();
        openNow_button.setText(R.string.open_now);
        openNow_button.setTextOff(null);
        openNow_button.setTextOn(null);
        filterLocation.onCommitCompletion(null);
         openNow_button.setOnClickListener(openNowListener());
        filterLocation.addTextChangedListener(createTextWatcherLocation());
        filterLocation.setOnItemClickListener(createOnItemClick());

        filterRadius.setOnSeekBarChangeListener(createOnSeekBarChangeListener());
        mapViewButton.setOnClickListener(toggleMapView());

        initVenueStatePageAdapter();
        setHasOptionsMenu(true);
        keyword = getArguments().getString("keyword");
        if (keyword != null && !keyword.trim().isEmpty() && !isChangedSearchText) {
            lastQuery = keyword;
            isQueryFromFastSearch=true;
        }
        this.setRetainInstance(true);
        currentPage = 0;
        price=0;
        // Post SearchEvent to EventBus
        EventBus.getDefault().post(new SearchEvent(true));

        return view;
    }

    private View.OnClickListener openNowListener() {
       return new View.OnClickListener(){
           @Override
           public void onClick(View v) {
               resetParameters();
               deepSearch();
           }
       };

    }

    public void setDefaultPriceToggleButton(){
        for(int i=0;i<this.prices.size();i++){
            ToggleButton price=this.prices.get(i);
            price.setText(null);
            price.setTextOn(null);
            price.setTextOff(null);
            price.setChecked(false);
            setOnClickToggleButtonPrice(price,i);
        }
    }

    public void getDrawable(){
        {
            selected_prices_background = getContext().getDrawable(getContext().getResources().getIdentifier("selected_price", "drawable", getContext().getPackageName()));
            unselected_prices_background = getContext().getDrawable(getContext().getResources().getIdentifier("unselected_price", "drawable", getContext().getPackageName()));
            selected_money=getContext().getDrawable(getContext().getResources().getIdentifier("ic_attach_money_coloraccent_24dp", "drawable", getContext().getPackageName()));
            unselected_money=getContext().getDrawable(getContext().getResources().getIdentifier("ic_attach_money_gray_24dp", "drawable", getContext().getPackageName()));
        }
    }
    public void initVenueStatePageAdapter() {
        venueStatePageAdapter = new VenueStatePageAdapter(getFragmentManager());
        venueStatePageAdapter.initVenuesFragment();
        venuesViewPager.setAdapter(venueStatePageAdapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      //  Toolbar toolbar = (Toolbar) view.findViewById(R.id.toolbar);
        inflater.inflate(R.menu.search_view, menu);
        final MenuItem item = menu.findItem(R.id.action_search);
        MenuItemCompat.expandActionView(item);
        searchView = (SearchView) MenuItemCompat.getActionView(item);
        searchView.setOnQueryTextListener(this);
        searchView.onActionViewExpanded();
        //searchView.requestFocus();
        searchView.clearFocus();
        searchView.setQuery(lastQuery, true);
        searchView.setOnSuggestionListener(new SearchView.OnSuggestionListener() {
            @Override
            public boolean onSuggestionClick(int position) {
                // updates search text to search box
                CursorAdapter cursorAdapter = searchView.getSuggestionsAdapter();
                Cursor cursor = cursorAdapter.getCursor();
                cursor.moveToPosition(position);
                searchView.setQuery(cursor.getString(cursor.getColumnIndex(KEY_WORD)),true);
                return true;
            }

            @Override
            public boolean onSuggestionSelect(int position) {
                return true;
            }
        });
        MenuItemCompat.setOnActionExpandListener(item,
                new MenuItemCompat.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        // Do something when collapsed
                        //Toast.makeText(getContext(),"back to fastSearch",Toast.LENGTH_SHORT).show();
                        getActivity().onBackPressed();
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        // Do something when expanded
                        //TODO
                        // displays the recommendation list
                        searchView.onActionViewExpanded();
                        //searchView.setQuery(lastQuery,false);
                        searchView.setQueryHint(getResources().getString(R.string.searching_question));
                        return true; // Return true to expand action view
                    }
                });

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.d(LOG, "Action: onQueryTextSubmit");
        searchView.clearFocus();
        if(!query.equals(lastQuery) || isQueryFromFastSearch) {
            isQueryFromFastSearch=false;
            getActivity().getCurrentFocus().clearFocus();
            resetParameters();
            deepSearch();
        }

        return false;
    }

    @Override
    public boolean onQueryTextChange(String typedKeyWord) {
        typedKeyWord=typedKeyWord.trim().toLowerCase(Locale.getDefault());
        if(!typedKeyWord.isEmpty()) {
            DaoSession daoSession = LocalDataBaseManager.getLocalDatabaseManager(getContext()).getDaoSession();
           List<SuggestionKeyWord> keyWords = daoSession.getSuggestionKeyWordDao().queryBuilder().distinct().list();
            if(keyWords!=null) {
                MatrixCursor matrixCursor = new MatrixCursor(new String[]{BaseColumns._ID, KEY_WORD});
                for (int i = 0; i < keyWords.size(); i++) {
                    String keyWord = keyWords.get(i).getSuggestionName().trim().toLowerCase();
                    if (keyWord.startsWith(typedKeyWord))
                        matrixCursor.addRow(new Object[]{i, keyWord});
                }

                keyWordsSuggestionAdapter.changeCursor(matrixCursor);
                searchView.setSuggestionsAdapter(keyWordsSuggestionAdapter);

            }
        }
        return false;
    }

    public boolean isReachedMaxVenues() {
        return reachedMaxVenues;
    }

    public void setReachedMaxVenues(boolean reachedMaxVenues) {
        this.reachedMaxVenues = reachedMaxVenues;
    }

    protected void deepSearch() {
        String query=searchView.getQuery().toString().trim();
        if (query == null || query.trim().isEmpty()) {
            query = lastQuery;
            if (searchView != null)
                searchView.setQuery(lastQuery, true);
        }
        if (query != null && !query.trim().isEmpty()) {
            // Searching for
            isChangedSearchText = true;
            lastQuery = query;
            searchView.setQuery(query, false);
            searchView.clearFocus();
            Log.d(LOG, "*** currentQuery: " + query);
            Log.d(LOG,"*** currentPageQuery: "+currentPage);
            VenueSearchQuery venueSearchQuery;
            if ( filterLocation!=null && !filterLocation.getText().toString().isEmpty()&& !filterLocation.getText().toString().equals(getContext().getResources().getString(R.string.near_me))) {
                venueSearchQuery = new VenueSearchQuery(query, filterLocation.getText().toString().trim());
            } else {
                // gets current location based on gps; "Near me"
                Location currentLocation=((MainActivity)getActivity()).getUserLocation();
                venueSearchQuery = new VenueSearchQuery(query, currentLocation.getLongitude(), currentLocation.getLatitude());
            }
            venueSearchQuery.setRadius(filterRadius.getProgress()*1000);
            lastOpenNow=openNow_button.isChecked();
            price=updatePrice();
            venueSearchQuery.setOnlyOpen(openNow_button.isChecked());
            venueSearchQuery.setPrice(price);
            VenueService venueService = ServiceFactory.createRetrofitService(VenueService.class, URL);
            venueService.queryVenue(venueSearchQuery,currentPage).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(venueSearchResult -> {
                                List venuesList = venueSearchResult.getResults();
                                Log.d(LOG,"*** number of venues: "+venuesList.size());
                                updateKeyWords(venuesList,lastQuery);
                               if(venuesList.size()>0) {
                                   venues.addAll(venuesList);
                                   if (mapViewButton.isChecked())
                                       displayVenuesList();
                                   else {
                                       //calls map services to display positions
                                       displayVenuesOnMap();
                                   }
                               }
                               else{

                                   if(this.getCurrentPage()>0) {
                                       this.setCurrentPage(getCurrentPage() - 1);
                                       reachedMaxVenues=true;
                                   }
                               }
                               if(progressDialog!=null)
                                   progressDialog.dismiss();

                            },
                            throwable -> {
                                //handle exception
                                if(progressDialog!=null)
                                    progressDialog.dismiss();
                                Log.d(LOG, throwable.toString());

                            }
                    );
        }
    }



    /**
     * listens the changes of location
     *
     * @return
     */
    public TextWatcher createTextWatcherLocation() {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String changedLocation = s.toString().trim();
                if (!changedLocation.equals(lastFilterLocation)) {
                    lastFilterLocation = changedLocation;
                    PlaceService placeService = ServiceFactory.createRetrofitService(PlaceService.class, URL);
                    placeService.getSuggestedPlaces(changedLocation)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(placeAutoComplete -> {
                                        List<Prediction> predictions = placeAutoComplete.getPredictions();
                                        if (placeAdapter == null || predictions.size() > 0) {
                                            if(placeAdapter==null) {
                                                placeAdapter = new PlaceAdapter(getContext(), predictions);
                                                filterLocation.setAdapter(placeAdapter);
                                            }else {
                                                filterLocation.invalidate();
                                                placeAdapter.clear();
                                                placeAdapter.addAll(predictions);

                                            }
                                            placeAdapter.notifyDataSetChanged();


                                        }

                                    },
                                    throwable -> {
                                        Log.d(LOG, "exception");

                                    });
                }
            }

        };
    }

    /**
     * listens the changes of seekbar
     *
     * @return
     */
    public SeekBar.OnSeekBarChangeListener createOnSeekBarChangeListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                seekBarView.setText(String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                resetParameters();
                deepSearch();
            }
        };
    }


    public View.OnClickListener toggleMapView() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (venues != null && venues.size() > 0) {
                    if (mapViewButton.isChecked()) {
                        // change toggle button to list view
                        // isMapView = false;
                        displayVenuesList();
                    } else {
                        //TODO
                        // change toggle button to map view
                        // isMapView = true;

                        displayVenuesOnMap();

                    }
                }
            }
        };

    }

    private void displayVenuesList() {
            VenuesListFragment venuesListFragment = (VenuesListFragment) venueStatePageAdapter.getItem(0);
            // handle back from venue in detail
            if (venuesListFragment.getView() == null) {
                initVenueStatePageAdapter();
                venuesListFragment = (VenuesListFragment) venueStatePageAdapter.getItem(0);
            }
            venuesListFragment.setParent(this);
            venuesListFragment.updateRecyclerView(venues);
            venuesViewPager.setCurrentItem(0);

    }

    public boolean isSubmitNewQuery() {
        return submitNewQuery;
    }

    public void setSubmitNewQuery(boolean submitNewQuery) {
        this.submitNewQuery = submitNewQuery;
    }

    private void displayVenuesOnMap() {
        VenuesOnMapFragment venuesOnMapFragment = (VenuesOnMapFragment) venueStatePageAdapter.getItem(1);
        // handle back from venue in detail
        if (venuesOnMapFragment.getView() == null) {
            initVenueStatePageAdapter();
            venuesOnMapFragment = (VenuesOnMapFragment) venueStatePageAdapter.getItem(1);
        }
        venuesOnMapFragment.updateVenuesMarker(venues);
        venuesViewPager.setCurrentItem(1);
    }


    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    @Override

    public void onPlaceSelected(Place place) {

    }

    @Override
    public void onError(Status status) {

    }

    public AdapterView.OnItemClickListener createOnItemClick() {
        return
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        String query = searchView.getQuery().toString();
                        if (!query.isEmpty()) {
                            resetParameters();
                            progressDialog = new ProgressDialog(getActivity(), 1);
                            progressDialog.setIndeterminate(true);
                            progressDialog.setMessage(getString(R.string.venue_search_progress_dialog));
                            progressDialog.show();
                            Prediction place = (Prediction) parent.getItemAtPosition(position);
                            filterLocation.setText(place.getDescription());
                            filterLocation.setSelection(filterLocation.getText().length());
                            deepSearch();
                        }

                    }
                };
    }
    private void resetParameters(){
        venues = new ArrayList<Venue>();
        submitNewQuery = true;
        currentPage = 0;
        reachedMaxVenues = false;
        initVenueStatePageAdapter();
    }


    private  void setOnClickToggleButtonPrice(ToggleButton toggleButton,int index){
        toggleButton.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Drawable icon;
                        Log.d(LOG," *** togglebutton state: "+toggleButton.isChecked());
                        if(toggleButton.isChecked()) {
                            icon = selected_prices_background;
                            toggleButton.setBackgroundDrawable(selected_money);
                             for(int i=0;i<index;i++){
                                 ToggleButton selectedPrice=prices.get(i);
                                 selectedPrice.setChecked(true);
                                 selectedPrice.setBackgroundDrawable(selected_money);
                             }
                            for(int i=index+1;i<prices.size();i++){
                                ToggleButton unselectedPrice=prices.get(i);
                                unselectedPrice.setChecked(false);
                                unselectedPrice.setBackgroundDrawable(unselected_money);
                            }
                        }
                        else {
                            icon = unselected_prices_background;
                            for(int i=0;i<prices.size();i++){
                                ToggleButton unselectedPrice=prices.get(i);
                                unselectedPrice.setChecked(false);
                                unselectedPrice.setBackgroundDrawable(unselected_money);
                            }
                        }
                        priceLinearLayout.setBackground(icon);
                        resetParameters();
                        deepSearch();
                    }

                });
    }


    public  int updatePrice(){
        if(price_5.isChecked())
            return 5;
        else if(price_4.isChecked())
            return 4;
       else if(price_3.isChecked())
            return 3;
        else if(price_2.isChecked())
            return 2;
        else if(price_1.isChecked())
            return 1;
        else return 0;

    }

    public void updateKeyWords(List<Venue> venues,String query){
        DaoSession daoSession = LocalDataBaseManager.getLocalDatabaseManager(getContext()).getDaoSession();
        List<SuggestionKeyWord> keyWords = daoSession.getSuggestionKeyWordDao().queryBuilder().list();
        List<String>extractedSuggestionNames=extractSuggestionName(keyWords);
        SuggestionKeyWord suggestionKeyWord;
        if(!isContains(extractedSuggestionNames,query)) {
            extractedSuggestionNames.add(query);
            suggestionKeyWord = new SuggestionKeyWord();
            suggestionKeyWord.setUid(UUID.randomUUID().toString());
            suggestionKeyWord.setSuggestionName(query);
            daoSession.getSuggestionKeyWordDao().insert(suggestionKeyWord);
        }
        for(Venue venue: venues){
            List<String> types=venue.getTypes();
            for(String type: types){
               String keyWord=type.replace("_"," ");
                if(!isContains(extractedSuggestionNames,keyWord)) {
                    extractedSuggestionNames.add(keyWord);
                    suggestionKeyWord = new SuggestionKeyWord();
                    suggestionKeyWord.setUid(UUID.randomUUID().toString());
                    suggestionKeyWord.setSuggestionName(keyWord);
                    daoSession.getSuggestionKeyWordDao().insert(suggestionKeyWord);
                }
            }
        }
    }

    public  List<String> extractSuggestionName(List<SuggestionKeyWord> keyWords){
        List<String>suggestionNames=new ArrayList<>();
        for(SuggestionKeyWord suggestionKeyWord:keyWords){
            suggestionNames.add(suggestionKeyWord.getSuggestionName());
        }
        return suggestionNames;
    }
    public boolean isContains(List<String> keyWords ,String keyword){
        for(String suggestionKeyWord:keyWords){
            if(suggestionKeyWord.trim().toLowerCase().equals(keyword.trim().toLowerCase()))
                return true;
        }
        return false;

    }

    public void onStop(){
        //Post SearchEvent to EventBus
        EventBus.getDefault().post(new SearchEvent(false));
        super.onStop();
    }

    /**
     * SearchEvent class to adjust Accuracy and Power depending on activity
     */
    public static class SearchEvent {
        public boolean isSearch;
        public SearchEvent(boolean b) {
            this.isSearch = b;
        }
    }


}
