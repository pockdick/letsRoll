package noevasher.letsroll.rute.controllers.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;

import butterknife.ButterKnife;
import noevasher.letsroll.R;
import noevasher.letsroll.commons.parents.fragments.BaseFragment;

public class RouteFragment extends BaseFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rutes, container, false); //setContentView(R.layout.fragment_login);
        ButterKnife.bind(this, view);


        return view;
    }
}
