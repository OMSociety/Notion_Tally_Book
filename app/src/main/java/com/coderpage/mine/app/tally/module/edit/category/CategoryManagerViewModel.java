package com.coderpage.mine.app.tally.module.edit.category;

import android.app.Activity;
import android.app.Application;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.OnLifecycleEvent;
import android.view.View;

import com.coderpage.base.common.IError;
import com.coderpage.base.common.SimpleCallback;
import com.coderpage.base.utils.ResUtils;
import com.coderpage.framework.BaseViewModel;
import com.coderpage.framework.ViewReliedTask;
import com.coderpage.mine.R;
import com.coderpage.mine.app.tally.eventbus.EventCategoryAdd;
import com.coderpage.mine.app.tally.eventbus.EventCategoryOrderChange;
import com.coderpage.mine.app.tally.eventbus.EventCategoryUpdate;
import com.coderpage.mine.app.tally.persistence.model.CategoryModel;
import com.coderpage.mine.ui.widget.PopupMenu;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

/**
 * @author lc. 2019-04-18 22:46
 * @since 0.6.0
 *
 * 分类管理页 ViewModel
 */

public class CategoryManagerViewModel extends BaseViewModel implements LifecycleObserver {

    private int mType = -1;
    private CategoryRepository mRepository;

    private MutableLiveData<List<CategoryModel>> mCategoryList = new MutableLiveData<>();
    private MutableLiveData<ViewReliedTask<Activity>> mViewReliedTask = new MutableLiveData<>();

    public CategoryManagerViewModel(Application application) {
        super(application);
        mRepository = new CategoryRepository();
    }

    LiveData<List<CategoryModel>> getCategoryList() {
        return mCategoryList;
    }

    LiveData<ViewReliedTask<Activity>> getViewReliedTask() {
        return mViewReliedTask;
    }

    /**
     * 修改显示的分类类型点击
     *
     * @param type {@link CategoryModel#TYPE_EXPENSE} {@link CategoryModel#TYPE_INCOME}
     */
    void onTypeChange(int type) {
        if (type == mType) {
            return;
        }
        mType = type;
        refreshData();
    }

    /** toolbar menu sort click */
    void onMenuSortClick(Activity activity, View shareRoot) {
        CategorySortActivity.open(activity, mType, shareRoot);
    }

    /** 分类 ITEM 菜单点击 */
    public void onItemMenuClick(View anchorView, CategoryModel category) {
        mViewReliedTask.setValue(activity -> {
            new PopupMenu(activity)
                    .addMenu(0, 0, ResUtils.getString(activity, R.string.edit_category))
                    .setOnItemClickListener((popupWindow, item) -> {
                        switch (item.getId()) {
                            case 0:
                                popupWindow.dismiss();
                                CategoryEditActivity.openAsEdit(activity, category.getId());
                                break;
                            default:
                                popupWindow.dismiss();
                                break;
                        }
                    })
                    .show(anchorView);
        });
    }

    /** 添加分类点击 */
    public void onAddCategoryClick() {
        mViewReliedTask.setValue(activity -> {
            CategoryEditActivity.openAsAddNew(activity, mType);
        });
    }

    private void refreshData() {
        if (mType == CategoryModel.TYPE_EXPENSE) {
            mRepository.loadAllExpenseCategory(new SimpleCallback<List<CategoryModel>>() {
                @Override
                public void success(List<CategoryModel> categoryModels) {
                    mCategoryList.postValue(categoryModels);
                }

                @Override
                public void failure(IError error) { }
            });
        }
        if (mType == CategoryModel.TYPE_INCOME) {
            mRepository.loadAllIncomeCategory(new SimpleCallback<List<CategoryModel>>() {
                @Override
                public void success(List<CategoryModel> categoryModels) {
                    mCategoryList.postValue(categoryModels);
                }

                @Override
                public void failure(IError error) { }
            });
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate(LifecycleOwner owner) {
        EventBus.getDefault().register(this);
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy(LifecycleOwner owner) {
        EventBus.getDefault().unregister(this);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventAddCategory(EventCategoryAdd event) {
        refreshData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventUpdateCategory(EventCategoryUpdate event) {
        refreshData();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventSortCategory(EventCategoryOrderChange event) {
        refreshData();
    }
}
