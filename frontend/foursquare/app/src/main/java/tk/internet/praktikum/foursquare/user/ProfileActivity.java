package tk.internet.praktikum.foursquare.user;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import tk.internet.praktikum.Constants;
import tk.internet.praktikum.foursquare.MainActivity;
import tk.internet.praktikum.foursquare.R;
import tk.internet.praktikum.foursquare.api.ImageCacheLoader;
import tk.internet.praktikum.foursquare.api.ImageSize;
import tk.internet.praktikum.foursquare.api.ServiceFactory;
import tk.internet.praktikum.foursquare.api.bean.Gender;
import tk.internet.praktikum.foursquare.api.bean.User;
import tk.internet.praktikum.foursquare.api.service.ChatService;
import tk.internet.praktikum.foursquare.api.service.ProfileService;
import tk.internet.praktikum.foursquare.api.service.UserService;
import tk.internet.praktikum.foursquare.chat.ChatActivity;
import tk.internet.praktikum.foursquare.search.SearchPersonActivity;
import tk.internet.praktikum.foursquare.storage.LocalStorage;

public class ProfileActivity extends AppCompatActivity {

    private final String URL = "https://dev.ip.stimi.ovh/";
    private User otherUser = new User();
    private TextView name, email, city, age;
    private RadioButton male, female, none;
    private ImageView avatarPicture;
    private FloatingActionButton fab;
    private String userID;

    public ProfileActivity() {}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        userID = getIntent().getStringExtra("userID");

        Toolbar toolbar = (Toolbar) findViewById(R.id.profile_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // TODO - Title = Name der Person?
        setTitle("Profile");

        name = (TextView) findViewById(R.id.profile_name);
        email = (TextView) findViewById(R.id.profile_email);
        city = (TextView) findViewById(R.id.profile_city);
        age = (TextView) findViewById(R.id.profile_age);

        male = (RadioButton) findViewById(R.id.radioButton);
        female = (RadioButton) findViewById(R.id.radioButton2);
        none = (RadioButton) findViewById(R.id.radioButton3);

        avatarPicture = (ImageView) findViewById(R.id.profile_activity_avatar);
        fab = (FloatingActionButton) findViewById(R.id.profile_activity_fab);

        initialiseFab();
        initialiseProfile();
    }

    private void initialiseFab() {
        ProfileService service = ServiceFactory
                .createRetrofitService(ProfileService.class, URL, LocalStorage.
                        getSharedPreferences(getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service.profileIfFriends(userID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            friendListResponse -> {
                                List<User> friendList = friendListResponse.getFriends();
                                boolean isFriend = false;

                                for (User user : friendList)
                                    if (user.getId().equals(userID)) {
                                        fab.setImageResource(R.mipmap.ic_chat_black_48dp);
                                        isFriend = true;
                                    }

                                    if  (isFriend) {
                                        fab.setOnClickListener(v -> startChat());
                                    }else {
                                        fab.setOnClickListener(v -> addFriend());
                                    }
                            },
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initialiseProfile() {
        UserService service = ServiceFactory
                .createRetrofitService(UserService.class, URL, LocalStorage.
                        getSharedPreferences(getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service.profileByID(userID)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            user -> {
                                otherUser = user;
                                name.setText(otherUser.getDisplayName());
                                email.setText(otherUser.getEmail());
                                city.setText(otherUser.getCity());
                                age.setText(Integer.toString(otherUser.getAge()));
                                Gender gender = otherUser.getGender();
                                if (gender == Gender.MALE)
                                    male.setChecked(true);
                                else if (gender == Gender.FEMALE)
                                    female.setChecked(true);
                                else
                                    none.setChecked(true);

                                if (otherUser.getAvatar() != null) {
                                    ImageCacheLoader imageCacheLoader = new ImageCacheLoader(getApplicationContext());
                                    imageCacheLoader.loadBitmap(otherUser.getAvatar(), ImageSize.LARGE)
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(bitmap -> {
                                                        avatarPicture.setImageBitmap(bitmap);
                                                    },
                                                    throwable -> {
                                                        Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                    }
                                            );
                                }
                            },
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addFriend() {
        UserService service = ServiceFactory
                .createRetrofitService(UserService.class, URL, LocalStorage.
                        getSharedPreferences(getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service.sendFriendRequest(otherUser.getName())
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            user -> {},
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startChat() {
        ChatService service = ServiceFactory
                .createRetrofitService(ChatService.class, URL, LocalStorage.
                        getSharedPreferences(getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service.getOrStartChat(userID)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            chatResponse -> {
                                chatResponse.getChatId();
                                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                                intent.putExtra("chatId", chatResponse.getChatId());
                                intent.putExtra("Parent", "ProfileActivity");
                                startActivity(intent);
                            },
                            throwable -> {
                                Toast.makeText(getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Intent getSupportParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    @Override
    public Intent getParentActivityIntent() {
        return getParentActivityIntentImpl();
    }

    private Intent getParentActivityIntentImpl() {
        Intent i = null;
        Bundle bundle = getIntent().getExtras();
        String parentActivity = bundle.getString("Parent");

        if (parentActivity.equals("UserActivity")) {
            i = new Intent(this, UserActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else if (parentActivity.equals("SearchPerson")) {
            i = new Intent(this, SearchPersonActivity.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
                i = new Intent(this, MainActivity.class);
                i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }

        return i;
    }
}
