package tk.internet.praktikum.foursquare;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.File;
import java.io.IOException;

import de.hdodenhof.circleimageview.CircleImageView;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import tk.internet.praktikum.CommentAdapter;
import tk.internet.praktikum.Constants;
import tk.internet.praktikum.foursquare.api.ImageCacheLoader;
import tk.internet.praktikum.foursquare.api.ImageSize;
import tk.internet.praktikum.foursquare.api.ServiceFactory;
import tk.internet.praktikum.foursquare.api.UploadHelper;
import tk.internet.praktikum.foursquare.api.bean.CheckinInformation;
import tk.internet.praktikum.foursquare.api.bean.Location;
import tk.internet.praktikum.foursquare.api.bean.TextComment;
import tk.internet.praktikum.foursquare.api.bean.Venue;
import tk.internet.praktikum.foursquare.api.service.UserService;
import tk.internet.praktikum.foursquare.api.service.VenueService;
import tk.internet.praktikum.foursquare.storage.LocalStorage;

public class NewVenueDetail extends AppCompatActivity implements OnMapReadyCallback {

    public static final String URL = "https://dev.ip.stimi.ovh/";
    public static final String LOG = NewVenueDetail.class.getSimpleName();
    public static final int REQUEST_PICTURE = 0;
    public static final int REQUEST_GALLERY = 1;

    private ImageView headerImage;
    private ProgressDialog progressDialog;
    private GoogleMap map;
    private Toolbar toolbar;
    private TextView infoVicinity;
    private ImageView image_1;
    private ImageView image_2;
    private ImageView image_3;

    private ImageView[] money;

    private RecyclerView commentRecyclerView;
    private CommentAdapter commentAdapter;

    private NestedScrollView scrollView;

    private Button callBtn, wwwBtn, checkinBtn;
    private FloatingActionMenu fabMenu;
    private FloatingActionButton fabTextComment;
    private String venueId;
    private FloatingActionButton fabImageComment;

    private TextView[] leaderboard_name;
    private TextView[] leaderboard_count;
    private CircleImageView[] leaderboard_avatar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_venue_detail);

        // Setup toolbar
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        Intent intent = getIntent();
        venueId = intent.getStringExtra("VENUE_ID");

        Log.d(LOG, "Display id: " + venueId);

        headerImage = (ImageView) findViewById(R.id.header_image);

        // Leaderboard
        leaderboard_name = new TextView[]{
                (TextView) findViewById(R.id.leaderboard_1_name),
                (TextView) findViewById(R.id.leaderboard_2_name),
                (TextView) findViewById(R.id.leaderboard_3_name)
        };

        leaderboard_count = new TextView[]{
                (TextView) findViewById(R.id.leaderboard_1_count),
                (TextView) findViewById(R.id.leaderboard_2_count),
                (TextView) findViewById(R.id.leaderboard_3_count)
        };

        leaderboard_avatar = new CircleImageView[]{
                (CircleImageView) findViewById(R.id.leaderboard_1_avatar),
                (CircleImageView) findViewById(R.id.leaderboard_2_avatar),
                (CircleImageView) findViewById(R.id.leaderboard_3_avatar)
        };

        // Buttons
        callBtn = (Button) findViewById(R.id.call);
        wwwBtn = (Button) findViewById(R.id.www);
        checkinBtn = (Button) findViewById(R.id.checkin);

        scrollView = (NestedScrollView) findViewById(R.id.scrollView);

        // Preview images
        image_1 = (ImageView) findViewById(R.id.image_1);
        image_2 = (ImageView) findViewById(R.id.image_2);
        image_3 = (ImageView) findViewById(R.id.image_3);

        // Comment recylcer view
        commentRecyclerView = (RecyclerView) findViewById(R.id.comment_recycler_view);

        commentRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.HORIZONTAL));

        commentAdapter = new CommentAdapter(venueId, getApplicationContext());
        RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());

        scrollView.setOnScrollChangeListener(new NestedScrollView.OnScrollChangeListener() {
            @Override
            public void onScrollChange(NestedScrollView v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                if (scrollY == (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())) {
                    Log.i(NewVenueDetail.class.getName(), "BOTTOM SCROLL");
                    commentAdapter.loadMore();
                }
            }
        });

        commentRecyclerView.setNestedScrollingEnabled(false);
        commentRecyclerView.setLayoutManager(mLayoutManager);
        commentRecyclerView.setItemAnimator(new DefaultItemAnimator());
        commentRecyclerView.setAdapter(commentAdapter);

        // Price indicator
        money = new ImageView[]{
                (ImageView) findViewById(R.id.money_1),
                (ImageView) findViewById(R.id.money_2),
                (ImageView) findViewById(R.id.money_3),
                (ImageView) findViewById(R.id.money_4),
                (ImageView) findViewById(R.id.money_5)
        };

        infoVicinity = (TextView) findViewById(R.id.text_vicinty);

        SupportMapFragment mapFragment = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapView));
        mapFragment.getMapAsync(this);

        progressDialog = new ProgressDialog(this, 0);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Spy is looking up details...");
        progressDialog.show();

        // FAB Buttons
        fabMenu = (FloatingActionMenu) findViewById(R.id.floating_menu);
        fabTextComment = (FloatingActionButton) findViewById(R.id.venue_detail_text_comment_button);
        fabImageComment = (FloatingActionButton) findViewById(R.id.venue_detail_image_commnent_button);
    }

    @Override
    protected void onStart() {
        super.onStart();

        fabTextComment.setOnClickListener(v -> openTextDialog());

        fabImageComment.setOnClickListener(v -> openImageDialog());

        // Load venue data from server
        VenueService venueService = ServiceFactory.createRetrofitService(VenueService.class, URL);
        venueService.getDetails(venueId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(venue -> {
                    progressDialog.dismiss();
                    toolbar.setTitle(venue.getName());
                    updateVicinty(venue);
                    updateVenueLocation(venue.getLocation());
                    updatePrice(venue);
                    updateButtons(venue);
                    updateLeaderboard(venue);
                    if (venue.getImages().size() > 0) {
                        ImageCacheLoader imageCacheLoader = new ImageCacheLoader(getApplicationContext());

                        imageCacheLoader.loadBitmap(venue.getImages().get(0), ImageSize.MEDIUM)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(bitmap -> {
                                    headerImage.setImageBitmap(bitmap);
                                });

                        if (venue.getImages().size() > 1)
                            imageCacheLoader.loadBitmap(venue.getImages().get(1), ImageSize.SMALL)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(bitmap -> {
                                        image_1.setImageBitmap(bitmap);
                                    });

                        if (venue.getImages().size() > 2)
                            imageCacheLoader.loadBitmap(venue.getImages().get(2), ImageSize.SMALL)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(bitmap -> {
                                        image_2.setImageBitmap(bitmap);
                                    });

                        if (venue.getImages().size() > 3)
                            imageCacheLoader.loadBitmap(venue.getImages().get(3), ImageSize.SMALL)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe(bitmap -> {
                                        image_3.setImageBitmap(bitmap);
                                    });
                    }
                }, err -> {
                    Log.d(NewVenueDetail.class.getName(), err.toString());
                });
    }

    private void updateLeaderboard(Venue venue) {
        UserService us = ServiceFactory.createRetrofitService(UserService.class, URL);
        for(int i = 0; i < 3 && i < venue.getTopCheckins().size(); i++) {
            CheckinInformation info = venue.getTopCheckins().get(i);
            leaderboard_count[i].setText(String.format("%d", info.getCount()));
            final int current = i;
            us.profileByID(info.getUserID())
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((res) -> {
                        leaderboard_name[current].setText(res.getName());
                        ImageCacheLoader icl = new ImageCacheLoader(getApplicationContext());
                        if(res.getAvatar() != null) {
                            icl.loadBitmap(res.getAvatar(), ImageSize.SMALL)
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe((bitmap) -> leaderboard_avatar[current].setImageBitmap(bitmap), (err) -> Log.d(LOG, err.toString(), err));
                        }
                    }, (err) -> Log.d(LOG, err.toString(), err));
        }
    }

    private void openTextDialog() {
        fabMenu.close(true);
        LocalStorage ls = LocalStorage.getLocalStorageInstance(getApplicationContext());
        SharedPreferences sp = LocalStorage.getSharedPreferences(getApplicationContext());
        if (!ls.isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Login first", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialDialog.Builder(this)
                .title("Post Comment")
                .inputType(InputType.TYPE_CLASS_TEXT)
                .input("Your awesome message!", "", (dialog, input) -> {
                    if (input.toString().isEmpty())
                        return;
                    VenueService vs = ServiceFactory.createRetrofitService(VenueService.class, URL, sp.getString(Constants.TOKEN, ""));
                    TextComment comment = new TextComment(input.toString());
                    vs.addTextComment(comment, venueId)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(
                                    (res) -> commentAdapter.addComment(res),
                                    (err) -> Log.d(LOG, err.toString())
                            );
                })
                .show();
    }

    private void openImageDialog() {
        fabMenu.close(true);
        LocalStorage ls = LocalStorage.getLocalStorageInstance(getApplicationContext());
        SharedPreferences sp = LocalStorage.getSharedPreferences(getApplicationContext());
        if (!ls.isLoggedIn()) {
            Toast.makeText(getApplicationContext(), "Login first", Toast.LENGTH_SHORT).show();
            return;
        }
        new MaterialDialog.Builder(this)
                .title("Add Image Comment")
                .items(new String[]{
                        "Take Picture",
                        "Choose from Gallery"
                })
                .itemsCallback((dialog, itemView, position, text) -> {
                    if(position == 0) {
                        Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        startActivityForResult(takePicture, REQUEST_PICTURE);
                    } else {
                        Intent chooseFromGallery = new Intent(Intent.ACTION_GET_CONTENT);
                        chooseFromGallery.setType("image/*");
                        startActivityForResult(Intent.createChooser(chooseFromGallery, "Select Picture"), REQUEST_GALLERY);
                    }
                })
                .show();
    }

    private void updateButtons(Venue venue) {
        if (venue.getWebsite() != null && !venue.getWebsite().isEmpty())
            wwwBtn.setVisibility(View.VISIBLE);
        else
            wwwBtn.setVisibility(View.GONE);

        if (venue.getPhoneNumber() != null && !venue.getPhoneNumber().isEmpty())
            callBtn.setVisibility(View.VISIBLE);
        else
            callBtn.setVisibility(View.GONE);

        checkinBtn.setOnClickListener((view) -> {
            LocalStorage ls = LocalStorage.getLocalStorageInstance(getApplicationContext());
            SharedPreferences sp = LocalStorage.getSharedPreferences(getApplicationContext());
            if (ls.isLoggedIn()) {
                VenueService vs = ServiceFactory.createRetrofitService(VenueService.class, URL, sp.getString(Constants.TOKEN, ""));
                vs.checkin(venue.getId())
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe((res) -> Toast.makeText(getApplicationContext(), "Checked in", Toast.LENGTH_SHORT).show(),
                                (err) -> {
                                });
            } else {
                Toast.makeText(getApplicationContext(), "Login first", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updatePrice(Venue venue) {
        switch (venue.getPrice()) {
            default:
            case 0:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                break;

            case 1:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                break;

            case 2:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                break;

            case 3:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                break;

            case 4:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.gray));
                break;

            case 5:
                money[0].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[1].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[2].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[3].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                money[4].setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.black));
                break;
        }
    }

    private void updateVicinty(Venue venue) {
        StringBuilder sb = new StringBuilder();
        if (venue.getFormattedAddress() != null && !venue.getFormattedAddress().isEmpty())
            sb.append(venue.getFormattedAddress()).append("\n");
        else if  (venue.getVicinity() != null && !venue.getVicinity().isEmpty())
            sb.append(venue.getVicinity()).append("\n");
        if (venue.getPhoneNumber() != null && !venue.getPhoneNumber().isEmpty())
            sb.append(venue.getPhoneNumber()).append("\n");
        if (venue.getWebsite() != null && !venue.getWebsite().isEmpty())
            sb.append(venue.getWebsite()).append("\n");
        sb.append(venue.getCheckInCount()+ " Checkins").append("\n");
        infoVicinity.setText(sb.toString());
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;
        map.getUiSettings().setAllGesturesEnabled(false);
    }

    public void updateVenueLocation(Location location) {
        LatLng venueLocation = new LatLng(location.getLatitude(), location.getLongitude());
        Bitmap imageBitmap = BitmapFactory.
                decodeResource(getResources(), getResources().getIdentifier("venue_location_marker", "mipmap", getApplicationContext().getPackageName()));

        map.addMarker(new MarkerOptions()
                .position(venueLocation))
                .setIcon(BitmapDescriptorFactory.fromBitmap(imageBitmap));
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(venueLocation, 14));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        SharedPreferences sp = LocalStorage.getSharedPreferences(getApplicationContext());
        VenueService vs = ServiceFactory.createRetrofitService(VenueService.class, URL, sp.getString(Constants.TOKEN, ""));
        switch (requestCode) {
            case REQUEST_PICTURE:
                if (resultCode == RESULT_OK) {
                    try {
                        Bitmap image = (Bitmap) data.getExtras().get("data");
                        MultipartBody.Part img = UploadHelper.createMultipartBodySync(image, getApplicationContext(), false);
                        vs.addImageComment(img, venueId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(imageComment -> commentAdapter.addComment(imageComment),
                                        throwable -> Log.d(LOG, throwable.toString())
                                );
                    } catch (Exception e) {
                        Log.d(LOG, e.toString());
                    }

                }
                break;

            case REQUEST_GALLERY:
                if(resultCode == RESULT_OK) {
                    try {
                        Uri uri = data.getData();
                        Bitmap image = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        MultipartBody.Part img = UploadHelper.createMultipartBodySync(image, getApplicationContext(), false);
                        vs.addImageComment(img, venueId)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(imageComment -> commentAdapter.addComment(imageComment),
                                        throwable -> Log.d(LOG, throwable.toString())
                                );
                    } catch (Exception e) {
                        Log.d(LOG, e.toString());
                    }
                }
                break;

            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }
}
