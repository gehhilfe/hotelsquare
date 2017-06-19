package tk.internet.praktikum.foursquare.login;

import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import tk.internet.praktikum.foursquare.R;

public class LoginActivity extends AppCompatActivity {
    private LoginFragment loginFragment;
    private RegisterFragment registerFragment;
    private RestorePasswordFragment restorePasswordFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        addFragment();
    }

    public void addFragment() {
        loginFragment = new LoginFragment();
        getFragmentManager().beginTransaction().add(R.id.fragment_container, loginFragment).commit();
    }

    public void changeFragment(int fragmentId) {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();

        switch (fragmentId) {
            case 0:
                fragmentTransaction.replace(R.id.fragment_container, loginFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case 1:
                if (registerFragment == null)
                    registerFragment = new RegisterFragment();
                fragmentTransaction.replace(R.id.fragment_container, registerFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
            case 2:
                if (restorePasswordFragment == null)
                    restorePasswordFragment = new RestorePasswordFragment();
                fragmentTransaction.replace(R.id.fragment_container, restorePasswordFragment);
                fragmentTransaction.addToBackStack(null);
                fragmentTransaction.commit();
                break;
        }
    }
}
