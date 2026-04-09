package com.coderpage.mine.app.tally.network;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * RequestInterceptor 单元测试
 *
 * @author test
 * @since 0.8.0
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestInterceptorTest {

    private RequestInterceptor interceptor;

    @Mock
    private Interceptor.Chain mockChain;

    @Mock
    private Request mockRequest;

    @Mock
    private Response mockResponse;

    @Before
    public void setUp() {
        interceptor = new RequestInterceptor();
        when(mockChain.request()).thenReturn(mockRequest);
        when(mockRequest.newBuilder()).thenReturn(mock Request.Builder());
        try {
            when(mockChain.proceed(any(Request.class))).thenReturn(mockResponse);
        } catch (IOException e) {
            fail("IOException should not be thrown");
        }
    }

    @Test
    public void testInterceptorReturnsResponse() throws IOException {
        Response response = interceptor.intercept(mockChain);
        assertNotNull(response);
    }

    @Test
    public void testInterceptorImplementsInterface() {
        assertTrue("RequestInterceptor should implement Interceptor", 
                   interceptor instanceof Interceptor);
    }

    @Test
    public void testInterceptCallsChainProceed() throws IOException {
        interceptor.intercept(mockChain);
        try {
            verify(mockChain, times(1)).proceed(any(Request.class));
        } catch (IOException e) {
            fail("IOException should not be thrown");
        }
    }
}
