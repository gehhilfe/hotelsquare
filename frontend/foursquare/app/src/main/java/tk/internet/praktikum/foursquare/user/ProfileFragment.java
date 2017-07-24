package tk.internet.praktikum.foursquare.user;


import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MultipartBody;
import tk.internet.praktikum.Constants;
import tk.internet.praktikum.foursquare.R;
import tk.internet.praktikum.foursquare.api.ImageCacheLoader;
import tk.internet.praktikum.foursquare.api.ImageSize;
import tk.internet.praktikum.foursquare.api.ServiceFactory;
import tk.internet.praktikum.foursquare.api.UploadHelper;
import tk.internet.praktikum.foursquare.api.bean.Gender;
import tk.internet.praktikum.foursquare.api.bean.User;
import tk.internet.praktikum.foursquare.api.service.ProfileService;
import tk.internet.praktikum.foursquare.api.service.UserService;
import tk.internet.praktikum.foursquare.storage.LocalStorage;

import static android.app.Activity.RESULT_OK;

public class ProfileFragment extends Fragment {
    private TextView name, email, password, city, age;
    private Button upload, edit, save;
    private RadioButton male, female, none;
    private ImageView avatarPicture;

    private static final String LOG_TAG = ProfileFragment.class.getSimpleName();
    private final String URL = "https://dev.ip.stimi.ovh/";
    private User currentUser;
    private Bitmap avatar;
    private boolean newPicture, changedPassword;

    private final int REQUEST_CAMERA = 0;
    private final int REQUEST_GALLERY = 1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        name = (TextView) view.findViewById(R.id.profile_name);
        email = (TextView) view.findViewById(R.id.profile_email);
        password = (TextView) view.findViewById(R.id.profile_password);
        city = (TextView) view.findViewById(R.id.profile_city);
        age = (TextView) view.findViewById(R.id.profile_age);

        upload = (Button) view.findViewById(R.id.profile_avatar_upload_btn);
        edit = (Button) view.findViewById(R.id.profile_tmp_edit_btn);
        save = (Button) view.findViewById(R.id.profile_tmp_save_btn);

        male = (RadioButton) view.findViewById(R.id.radioButton);
        female = (RadioButton) view.findViewById(R.id.radioButton2);
        none = (RadioButton) view.findViewById(R.id.radioButton3);

        avatarPicture = (ImageView) view.findViewById(R.id.profile_avatar);

        upload.setOnClickListener(v -> uploadPicture());
        edit.setOnClickListener(v -> edit());
        save.setOnClickListener(v -> save());

        view.clearFocus();
        initialiseProfile();
        return view;
    }

    private void initialiseProfile() {
        ProfileService service = ServiceFactory
                .createRetrofitService(ProfileService.class, URL, LocalStorage.
                        getSharedPreferences(getActivity().getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service.profile()
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                            user -> {
                                currentUser = user;
                                name.setText(currentUser.getDisplayName());
                                email.setText(currentUser.getEmail());
                                city.setText(currentUser.getCity());
                                age.setText(Integer.toString(currentUser.getAge()));
                                Gender gender = currentUser.getGender();
                                if (gender == Gender.MALE)
                                    male.setChecked(true);
                                else if (gender == Gender.FEMALE)
                                    female.setChecked(true);
                                else
                                    none.setChecked(true);


                            if (currentUser.getAvatar() != null) {
                                ImageCacheLoader imageCacheLoader = new ImageCacheLoader(this.getContext());
                                imageCacheLoader.loadBitmap(currentUser.getAvatar(), ImageSize.LARGE)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe(bitmap -> {
                                            avatarPicture.setImageBitmap(bitmap);
                                        },
                                                throwable -> {
                                                    Toast.makeText(getActivity().getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                                }
                                        );
                            }
                            },
                            throwable -> {
                                Toast.makeText(getActivity().getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void save() {
        /*
        name.setEnabled(false);
        email.setEnabled(false);
        */
        password.setEnabled(false);
        city.setEnabled(false);
        age.setEnabled(false);

        /*
        name.clearFocus();
        email.clearFocus();
        */
        password.clearFocus();
        city.clearFocus();
        age.clearFocus();

        upload.setEnabled(false);
        save.setEnabled(false);

        male.setEnabled(false);
        female.setEnabled(false);
        none.setEnabled(false);

        if (password.getText() != "")
            currentUser.setPassword(password.getText().toString());

        /*
        currentUser.setName(name.getText().toString());
        currentUser.setEmail(email.getText().toString());
        */
        currentUser.setCity(city.getText().toString());
        currentUser.setAge(Integer.parseInt(age.getText().toString()));

        if (male.isChecked())
            currentUser.setGender(Gender.MALE);
        else if (female.isChecked())
            currentUser.setGender(Gender.FEMALE);
        else
            currentUser.setGender(Gender.UNSPECIFIED);

        uploadChanges();
    }

    private void uploadChanges() {
        ProfileService service = ServiceFactory
                .createRetrofitService(ProfileService.class, URL, LocalStorage.
                        getSharedPreferences(getActivity().getApplicationContext()).getString(Constants.TOKEN, ""));

        if  (newPicture) {
            try {
                MultipartBody.Part img = UploadHelper.createMultipartBodySync(avatar, getContext(), true);
                service.uploadAvatar(img)
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(user -> {
                            Log.d(LOG_TAG, "AVATAR ID" + user.getAvatar().getId());
                                },
                                throwable -> {
                                    Toast.makeText(getActivity().getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                        );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        UserService service2 = ServiceFactory
                .createRetrofitService(UserService.class, URL, LocalStorage.
                        getSharedPreferences(getActivity().getApplicationContext()).getString(Constants.TOKEN, ""));

        try {
            service2.update(currentUser)
                    .subscribeOn(Schedulers.newThread())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(user -> {
                        currentUser = user;
                            },
                            throwable -> {
                                Toast.makeText(getActivity().getApplicationContext(), throwable.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void edit() {
        /*
        name.setFocusable(true);
        email.setFocusable(true);
        */
        password.setFocusable(true);
        city.setFocusable(true);
        age.setFocusable(true);

        /*
        name.setEnabled(true);
        email.setEnabled(true);
        */
        password.setEnabled(true);
        city.setEnabled(true);
        age.setEnabled(true);

        upload.setEnabled(true);
        save.setEnabled(true);

        male.setEnabled(true);
        female.setEnabled(true);
        none.setEnabled(true);

        changedPassword = false;
        newPicture = false;
    }

    private void uploadPicture() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final String[] options = {"Camera", "Gallery", "Cancel"};
        builder.setTitle("Select an option to choose your avatar.");
        builder.setItems(options, (dialog, option) -> {
            switch (options[option]) {
                case "Camera":
                    Log.d(LOG_TAG, "Camera");
                    cameraIntent();
                    break;
                case "Gallery":
                    Log.d(LOG_TAG, "gallery");
                    galleryIntent();
                    break;
                case "Cancel":
                    dialog.dismiss();
                    break;
            }
        });
        builder.show();
    }

    private void cameraIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(intent, REQUEST_CAMERA);
    }

    private void galleryIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);//
        startActivityForResult(Intent.createChooser(intent, "Select File"), REQUEST_GALLERY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQUEST_CAMERA:
                if (resultCode == RESULT_OK) {
                    avatar = (Bitmap) data.getExtras().get("data");
                    avatarPicture.setImageBitmap(avatar);
                    newPicture = true;
                }
            break;

            case REQUEST_GALLERY:
                if (resultCode == RESULT_OK) {
                    try {
                        avatar = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), data.getData());
                        avatarPicture.setImageBitmap(avatar);
                        newPicture = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            break;
        }
    }
}
