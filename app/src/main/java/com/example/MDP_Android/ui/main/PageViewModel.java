package com.example.MDP_Android.ui.main;

import androidx.arch.core.util.Function;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;

public class PageViewModel extends ViewModel {

    private MutableLiveData<Integer> mutableLiveDataDate = new MutableLiveData<>();
    private LiveData<String> liveData = Transformations.map(mutableLiveDataDate, new Function<Integer, String>() {
        @Override
        public String apply(Integer input) {
            return "Hello world from section: " + input;
        }
    });

    public void setIndex(int index) {
        mutableLiveDataDate.setValue(index);
    }

    public LiveData<String> getText() {
        return liveData;
    }
}