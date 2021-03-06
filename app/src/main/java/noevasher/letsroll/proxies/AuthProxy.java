package noevasher.letsroll.proxies;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.ServerValue;

import java.util.HashMap;

import io.reactivex.Single;
import noevasher.letsroll.models.UserModel;
import noevasher.letsroll.services.Utilities;

public class AuthProxy extends AFirebase {

    private FirebaseAuth mAuth;
    private static AuthProxy authProxy;
    private DatabaseProxy mDatabaseProxy;

    public static AuthProxy getInstance(Context context) {
        if (authProxy == null) {
            authProxy = new AuthProxy(context);
        }
        return authProxy;
    }

    private AuthProxy(Context context) {
        super(context);
        FirebaseApp.initializeApp(context);
        mAuth = FirebaseAuth.getInstance();
        mDatabaseProxy = DatabaseProxy.getInstance(context);
        //this.mAndroid_id = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public Single<Object> createAccount(String name, String email, int age, String password) {
        //mAuth = FirebaseAuth.getInstance();
        return Single.create(e -> mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        assert user != null;
                        writeNewUser(user.getUid(), name, email, age, null);

                        UserProfileChangeRequest profileUpdates
                                = new UserProfileChangeRequest.Builder()
                                .setDisplayName(name).build();
                        user.updateProfile(profileUpdates);

                        e.onSuccess(user);
                    } else if (task.getException() instanceof FirebaseAuthUserCollisionException) {
                        e.onSuccess("duplicated");
                    } else {
                        e.onSuccess("false");
                    }

                }));
    }

    public Single<Object> startSessionUser(String email, String password) {
        //mAuth = FirebaseAuth.getInstance();
        return Single.create(e -> mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        FirebaseUser user = mAuth.getCurrentUser();
                        e.onSuccess(user);
                    } else {
                        // If sign in fails, display a message to the user.
                        if (task.getException() instanceof FirebaseAuthInvalidCredentialsException) {
                            //e.onError(task.getException());
                            e.onSuccess("false");

                        } else if (task.getException() instanceof
                                FirebaseAuthInvalidCredentialsException) {
                            checkProviders(email, "facebook.com").subscribe(provider -> {
                                if (!provider) {
                                    e.onSuccess(email);
                                } else {
                                    e.onSuccess("false");
                                }
                            });
                        } else {
                            e.onSuccess("false");
                        }
                    }
                }));
    }

    private Single<Boolean> checkProviders(String email, String provider) {
        return Single.create(e -> {
            mAuth.fetchSignInMethodsForEmail(email)
                    .addOnCompleteListener(task -> {
                                if (task.getResult().getSignInMethods() != null) {
                                    for (int i = 0; i < task.getResult().getSignInMethods().size(); i++) {
                                        String providerTask = task.getResult().getSignInMethods().get(i);
                                        if (providerTask.equals(provider)) {
                                            e.onSuccess(false);
                                            break;
                                        }
                                    }
                                }
                                e.onSuccess(true);
                            }
                    );
        });
    }

    private void writeNewUser(String userId, String name, String email, int age, String urlPicture) {
        UserModel userModel = createUser(name, email, age, userId, urlPicture);
        HashMap<String, Object> userData = new HashMap<>();
        userData.put("general", userModel.getGeneral());
        userData.put("summary", userModel.getSummary());
        mDatabaseProxy.updateChildrenSingle("users/" + userId, userData).subscribe(response -> {

        }, t -> {
            t.getMessage();
        });

        //userDatabaseReference.child("users/" + userId).setValue(userModel.getModelMap());
        //mDataBase.keepSynced(DBPaths.USERS, userModel.userId);
    }

    public void writeUserInfo(String userId, HashMap<String, Object> userData) {
        mDatabaseProxy.updateChildrenSingle("users/" + userId, userData).subscribe(response -> {
            Utilities.showMessage(mContext, "write...", false);
        }, t -> {
            Utilities.showMessage(mContext, t.getMessage(), true);

        });

    }


    public void writeInformation(String path, HashMap<String, Object> information) {
        mDatabaseProxy.updateChildrenSingle(path, information).subscribe(response -> {
            Utilities.showMessage(mContext, "save", false);
        }, t -> {
            Utilities.showMessage(mContext, t.getMessage(), true);

        });

    }



    public Single<Object> getFirebaseUser() {
        return Single.create(e -> {
            FirebaseUser user = mAuth.getCurrentUser();
            if (user != null) {
                if (user.getDisplayName() == null || user.getDisplayName().equals("")) {
                    user = mAuth.getCurrentUser();
                }
                e.onSuccess(user);
            } else {
                e.onSuccess("null");
            }
        });
    }

    private UserModel createUser(String name, String email, int age, String userId, String urlPicture) {

        HashMap<String, Object> summary = new HashMap<>();
        HashMap<String, Object> general = new HashMap<>();

        summary.put("email", email);
        summary.put("username", name);
        summary.put("age", age);
        summary.put("lastActivity", ServerValue.TIMESTAMP);
        summary.put("langCode", "es-MX");

        general.put("creationDate", ServerValue.TIMESTAMP);
        general.put("status", "online");
        general.put("type", "user");
        
        /*
        if(urlPicture != null && !urlPicture.equals(""))
            summary.put("lastUpdatedPicture", Long.toString( new Timestamp(System.currentTimeMillis()).getTime()));
        
        //*/
        UserModel userModel = new UserModel(general, summary);
        userModel.setUserId(userId);

        return userModel;
    }

    public void logout() {
        mAuth.signOut();
    }
}
