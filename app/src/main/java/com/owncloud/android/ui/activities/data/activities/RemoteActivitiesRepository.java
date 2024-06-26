/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2018 Edvard Holst <edvard.holst@gmail.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.owncloud.android.ui.activities.data.activities;

import com.nextcloud.common.NextcloudClient;

import java.util.List;

import androidx.annotation.NonNull;

public class RemoteActivitiesRepository implements ActivitiesRepository {

    private final ActivitiesServiceApi activitiesServiceApi;

    public RemoteActivitiesRepository(@NonNull ActivitiesServiceApi activitiesServiceApi) {
        this.activitiesServiceApi = activitiesServiceApi;
    }


    @Override
    public void getActivities(int lastGiven, @NonNull LoadActivitiesCallback callback) {
        activitiesServiceApi.getAllActivities(lastGiven,
                                              new ActivitiesServiceApi.ActivitiesServiceCallback<List<Object>>() {
            @Override
            public void onLoaded(List<Object> activities, NextcloudClient client, int lastGiven) {
                callback.onActivitiesLoaded(activities, client, lastGiven);
            }

            @Override
            public void onError(String error) {
                callback.onActivitiesLoadedError(error);
            }
        });
    }
}
