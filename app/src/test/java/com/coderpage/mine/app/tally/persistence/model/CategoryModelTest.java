package com.coderpage.mine.app.tally.persistence.model;

import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

import com.coderpage.mine.app.tally.persistence.sql.entity.CategoryEntity;

/**
 * CategoryModel 分类模型单元测试
 *
 * @author test
 * @since 0.8.0
 */
public class CategoryModelTest {

    private CategoryModel category;

    @Before
    public void setUp() {
        category = new CategoryModel();
    }

    @Test
    public void testTypeConstants() {
        assertEquals(CategoryEntity.TYPE_EXPENSE, CategoryModel.TYPE_EXPENSE);
        assertEquals(CategoryEntity.TYPE_INCOME, CategoryModel.TYPE_INCOME);
    }

    @Test
    public void testDefaultValues() {
        CategoryModel defaultCategory = new CategoryModel();
        assertEquals("", defaultCategory.getUniqueName());
        assertEquals("", defaultCategory.getName());
        assertEquals("", defaultCategory.getIcon());
        assertEquals(0, defaultCategory.getOrder());
        assertEquals(0, defaultCategory.getId());
        assertEquals(0, defaultCategory.getAccountId());
        assertEquals(0, defaultCategory.getSyncStatus());
    }

    @Test
    public void testSetAndGetId() {
        category.setId(100L);
        assertEquals(100L, category.getId());
    }

    @Test
    public void testSetAndGetUniqueName() {
        String uniqueName = "food_breakfast";
        category.setUniqueName(uniqueName);
        assertEquals(uniqueName, category.getUniqueName());
    }

    @Test
    public void testSetAndGetName() {
        String name = "早餐";
        category.setName(name);
        assertEquals(name, category.getName());
    }

    @Test
    public void testSetAndGetIcon() {
        String icon = "ic_food";
        category.setIcon(icon);
        assertEquals(icon, category.getIcon());
    }

    @Test
    public void testSetAndGetOrder() {
        category.setOrder(5);
        assertEquals(5, category.getOrder());
    }

    @Test
    public void testSetAndGetType() {
        category.setType(CategoryModel.TYPE_EXPENSE);
        assertEquals(CategoryModel.TYPE_EXPENSE, category.getType());

        category.setType(CategoryModel.TYPE_INCOME);
        assertEquals(CategoryModel.TYPE_INCOME, category.getType());
    }

    @Test
    public void testSetAndGetAccountId() {
        category.setAccountId(1L);
        assertEquals(1L, category.getAccountId());
    }

    @Test
    public void testSetAndGetSyncStatus() {
        category.setSyncStatus(CategoryEntity.SYNC_STATUS_SYNCED);
        assertEquals(CategoryEntity.SYNC_STATUS_SYNCED, category.getSyncStatus());
    }

    @Test
    public void testFullCategoryLifecycle() {
        category.setId(1L);
        category.setUniqueName("transport_bus");
        category.setName("公交车");
        category.setIcon("ic_bus");
        category.setOrder(3);
        category.setType(CategoryModel.TYPE_EXPENSE);
        category.setAccountId(1L);
        category.setSyncStatus(CategoryEntity.SYNC_STATUS_SYNCED);

        assertEquals(1L, category.getId());
        assertEquals("transport_bus", category.getUniqueName());
        assertEquals("公交车", category.getName());
        assertEquals("ic_bus", category.getIcon());
        assertEquals(3, category.getOrder());
        assertEquals(CategoryModel.TYPE_EXPENSE, category.getType());
        assertEquals(1L, category.getAccountId());
        assertEquals(CategoryEntity.SYNC_STATUS_SYNCED, category.getSyncStatus());
    }

    @Test
    public void testExpenseCategory() {
        category.setType(CategoryModel.TYPE_EXPENSE);
        assertEquals(CategoryModel.TYPE_EXPENSE, category.getType());
    }

    @Test
    public void testIncomeCategory() {
        category.setType(CategoryModel.TYPE_INCOME);
        assertEquals(CategoryModel.TYPE_INCOME, category.getType());
    }
}
