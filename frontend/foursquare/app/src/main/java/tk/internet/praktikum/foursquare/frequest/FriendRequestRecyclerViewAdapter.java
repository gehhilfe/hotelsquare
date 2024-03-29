package tk.internet.praktikum.foursquare.frequest;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import tk.internet.praktikum.Constants;
import tk.internet.praktikum.foursquare.R;
import tk.internet.praktikum.foursquare.api.ImageCacheLoader;
import tk.internet.praktikum.foursquare.api.ImageSize;
import tk.internet.praktikum.foursquare.api.ServiceFactory;
import tk.internet.praktikum.foursquare.api.bean.FriendRequest;
import tk.internet.praktikum.foursquare.api.bean.FriendRequestResponse;
import tk.internet.praktikum.foursquare.api.bean.User;
import tk.internet.praktikum.foursquare.api.service.ProfileService;
import tk.internet.praktikum.foursquare.storage.LocalStorage;
import tk.internet.praktikum.foursquare.user.ProfileActivity;

public class FriendRequestRecyclerViewAdapter extends RecyclerView.Adapter<FriendRequestRecyclerViewAdapter.HomeViewHolder> {

    class HomeViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        private TextView request;
        private ImageView accept, decline, avatar;

        public HomeViewHolder(View itemView) {
            super(itemView);
            request = (TextView) itemView.findViewById(R.id.home_entry_text);
            accept = (ImageView) itemView.findViewById(R.id.home_friend_request_accept);
            decline = (ImageView) itemView.findViewById(R.id.home_friend_request_decline);
            avatar = (ImageView) itemView.findViewById(R.id.home_avatar);

            itemView.setOnClickListener(this);
            accept.setOnClickListener(this);
            decline.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == R.id.home_friend_request_accept) {
                acceptFriendRequest();
            } else if (v.getId() == R.id.home_friend_request_decline) {
                declineFriendRequest();
            } else {
                loadProfile();
            }
        }

        /**
         * Loads the profile of the selected user.
         */
        private void loadProfile() {
            Intent intent = new Intent(context, ProfileActivity.class);
            intent.putExtra("userID", friendRequestList.get(getAdapterPosition()).getSenderID());
            intent.putExtra("Parent", "UserActivity");
            activity.startActivity(intent);
        }

        /**
         * Accepts the selected friend request
         */
        private void acceptFriendRequest() {
            ProfileService service = ServiceFactory
                    .createRetrofitService(ProfileService.class, URL, LocalStorage.
                            getSharedPreferences(context).getString(Constants.TOKEN, ""));

            try {
                service.answerFriendRequest(idNameMap.get(friendRequestList.get(getAdapterPosition()).getSenderID()), new FriendRequestResponse(true))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                user -> {
                                    if (context != null) {
                                        Toast.makeText(context, context.getString(R.string.accept_friend_request), Toast.LENGTH_SHORT).show();
                                    }
                                        friendRequestList.remove(getAdapterPosition());
                                    notifyDataSetChanged();
                                },
                                throwable -> Log.d(LOG, throwable.getMessage())
                        );
            }catch (Exception e) {
                e.printStackTrace();
            }
        }

        /**
         * Declines the selected friend request.
         */
        private void declineFriendRequest() {
            ProfileService service = ServiceFactory
                    .createRetrofitService(ProfileService.class, URL, LocalStorage.
                            getSharedPreferences(context).getString(Constants.TOKEN, ""));

            try {
                service.answerFriendRequest(idNameMap.get(friendRequestList.get(getAdapterPosition()).getSenderID()), new FriendRequestResponse(false))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(
                                user -> {
                                    if (context != null) {
                                        Toast.makeText(context, context.getString(R.string.decline_friend_request), Toast.LENGTH_SHORT).show();
                                    }
                                    friendRequestList.remove(getAdapterPosition());
                                    notifyDataSetChanged();
                                },
                                throwable -> Log.d(LOG, throwable.getMessage())
                        );
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static final String LOG = FriendRequestRecyclerViewAdapter.class.getSimpleName();
    private Context context;
    private LayoutInflater inflater;
    private List<FriendRequest> friendRequestList;
    private List<User> userList;
    private HashMap<String, String> idNameMap = new HashMap<>();
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("d.M.y HH:mm",  Locale.ENGLISH);
    private final String URL = "https://dev.ip.stimi.ovh/";
    private Activity activity;

    FriendRequestRecyclerViewAdapter(Context context, Activity activity) {
        inflater = LayoutInflater.from(context);
        this.activity = activity;
        this.context = context;
        friendRequestList = new ArrayList<>();
        userList = new ArrayList<>();
    }

    @Override
    public HomeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.home_entry, parent, false);
        return new HomeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(HomeViewHolder holder, int position) {
        FriendRequest currentFriendRequest = friendRequestList.get(position);
        User currentUser = new User();

        for (User user : userList)
            if (Objects.equals(user.getId(), currentFriendRequest.getSenderID())) {
                currentUser = user;
                idNameMap.put(friendRequestList.get(position).getSenderID(), user.getName());
            }

        // Load the avatar.
        if (currentUser.getAvatar() != null) {
            ImageCacheLoader imageCacheLoader = new ImageCacheLoader(context);
            imageCacheLoader.loadBitmap(currentUser.getAvatar(), ImageSize.LARGE)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(bitmap -> {
                                holder.avatar.setImageBitmap(bitmap);
                            },
                            throwable -> Log.d(LOG, throwable.getMessage())
                    );
        } else {
            holder.avatar.setImageResource(R.mipmap.user_avatar);
        }

        holder.request.setText(SIMPLE_DATE_FORMAT.format(currentFriendRequest.getCreatedAt()) + "\n" + currentUser.getName());
    }

    @Override
    public int getItemCount() {
        return friendRequestList.size();
    }

    /**
     * Updates the current list of friend requests.
     * @param requests new friend requests.
     * @param users list of persons who sent the requests.
     */
    void updateData(List<FriendRequest> requests, List<User> users) {
        friendRequestList.addAll(requests);
        userList.addAll(users);
        notifyDataSetChanged();
    }

    /**
     * Returns the current list of friend requests.
     * @return Current friend requests.
     */
    List<User> getUserList() {
        return userList;
    }
}
