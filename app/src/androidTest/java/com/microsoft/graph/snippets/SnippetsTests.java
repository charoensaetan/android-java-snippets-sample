/*
 * Copyright (c) Microsoft. All rights reserved. Licensed under the MIT license.
 * See LICENSE in the project root for license information.
 */
package com.microsoft.graph.snippets;

import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingResource;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.espresso.web.webdriver.DriverAtoms;
import android.support.test.espresso.web.webdriver.Locator;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.microsoft.graph.snippets.snippet.AbstractSnippet;

import junit.framework.AssertionFailedError;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.openActionBarOverflowOrOptionsMenu;
import static android.support.test.espresso.Espresso.registerIdlingResources;
import static android.support.test.espresso.Espresso.unregisterIdlingResources;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.ComponentNameMatchers.hasShortClassName;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.intent.matcher.IntentMatchers.toPackage;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static android.support.test.espresso.web.sugar.Web.onWebView;
import static android.support.test.espresso.web.webdriver.DriverAtoms.clearElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.findElement;
import static android.support.test.espresso.web.webdriver.DriverAtoms.webClick;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.IsAnything.anything;

@RunWith(AndroidJUnit4.class)
public class SnippetsTests {
    private static final String USER_ID_TEXT_ELEMENT = "cred_userid_inputtext";
    private static final String PASSWORD_TEXT_ELEMENT = "cred_password_inputtext";
    private static final String SIGN_IN_BUTTON_ELEMENT = "cred_sign_in_button";
    private static TestCredentials testCredentials;

    @Rule
    public ActivityTestRule<SignInActivity> mSignInActivityRule = new ActivityTestRule<>(SignInActivity.class, false, false);
    @Rule
    public ActivityTestRule<SnippetListActivity> mSnippetListActivityRule = new ActivityTestRule<>(SnippetListActivity.class, false, false);
    @Rule
    public ActivityTestRule<SnippetDetailActivity> mSnippetDetailActivityRule = new ActivityTestRule<>(SnippetDetailActivity.class, false, false);

    @BeforeClass
    public static void setupEnvironment() throws FileNotFoundException {
        testCredentials = TestCredentials.getTestCredentials();
        ServiceConstants.CLIENT_ID = testCredentials.clientId;
    }

    @Before
    public void initIntents(){
        Intents.init();
    }

    @After
    public void releaseIntents() {
        Intents.release();
    }

    @Test
    public void RunSnippets() throws InterruptedException{
        AzureADSignIn(testCredentials.username, testCredentials.password, mSignInActivityRule);

        List<Integer> snippetIndexes = getSnippetsIndexes(mSnippetListActivityRule);

        for(int index : snippetIndexes) {
            runSnippet(index);
        }
    }

    private void AzureADSignIn(String username, String password, ActivityTestRule<SignInActivity> signInActivityTestRule) throws InterruptedException {
        SignInActivity signInActivity = signInActivityTestRule.launchActivity(null);

        onView(withId(R.id.o365_signin)).perform(click());

        try {
            onWebView()
                    .withElement(findElement(Locator.ID, USER_ID_TEXT_ELEMENT))
                    .perform(clearElement())
                    // Enter text into the input element
                    .perform(DriverAtoms.webKeys(username))
                    // Set focus on the username input text
                    // The form validates the username when this field loses focus
                    .perform(webClick())
                    .withElement(findElement(Locator.ID, PASSWORD_TEXT_ELEMENT))
                    // Now we force focus on this element to make
                    // the username element to lose focus and validate
                    .perform(webClick())
                    .perform(clearElement())
                    // Enter text into the input element
                    .perform(DriverAtoms.webKeys(password));

            Thread.sleep(2000, 0);

            onWebView()
                    .withElement(findElement(Locator.ID, SIGN_IN_BUTTON_ELEMENT))
                    .perform(webClick());
        } catch (NoMatchingViewException ex) {
            // If user is already logged in, the flow will go directly to SnippetListActivity
        } finally {
            Thread.sleep(2000, 0);
        }

        // Finally, verify that SnippetListActivity is on top
        intended(allOf(
                hasComponent(hasShortClassName(".SnippetListActivity")),
                toPackage("com.microsoft.graph.snippets")
        ));

        signInActivity.finish();
    }

    private List<Integer> getSnippetsIndexes(ActivityTestRule<SnippetListActivity> snippetListActivityRule) {
        SnippetListActivity snippetListActivity = snippetListActivityRule.launchActivity(null);

        ListAdapter listAdapter = ((ListView) snippetListActivity
                .getSupportFragmentManager()
                .findFragmentById(R.id.snippet_list)
                .getView()
                .findViewById(android.R.id.list))
                .getAdapter();
        int numItems = listAdapter.getCount();

        List<Integer> snippetIndexes = new ArrayList<>();

        // Get the index of items in the adapter that
        // are actual snippets and not headers
        for (int i = 0; i < numItems; i++) {
            if(((AbstractSnippet)listAdapter.getItem(i)).getUrl() != null) {
                snippetIndexes.add(i);
            }
        }

        return snippetIndexes;
    }

    private void runSnippet(int index) {
        Intent itemIdIntent = new Intent();
        itemIdIntent.putExtra("item_id", index);
        SnippetDetailActivity snippetDetailActivity = mSnippetDetailActivityRule.launchActivity(itemIdIntent);

        SnippetDetailActivityIdlingResource idlingResource = new SnippetDetailActivityIdlingResource(snippetDetailActivity);
        registerIdlingResources(idlingResource);

        onView(withId(R.id.btn_run)).perform(click());

        onView(withId(R.id.txt_status_color)).check(matches(withContentDescription(R.string.stoplight_success)));
        unregisterIdlingResources(idlingResource);
        snippetDetailActivity.finish();
    }

    private class SnippetDetailActivityIdlingResource implements IdlingResource {
        private SnippetDetailActivity activity;
        private ResourceCallback callback;

        public SnippetDetailActivityIdlingResource(SnippetDetailActivity activity) {
            this.activity = activity;
        }

        @Override
        public String getName() {
            return this.getClass().getName();
        }

        @Override
        public boolean isIdleNow() {
            Boolean idle = activity != null &&
                    callback != null &&
                    !activity.isExecutingRequest();
            if (idle) callback.onTransitionToIdle();
            return idle;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            this.callback = resourceCallback;
        }
    }
}