package com.example.ecommerce.pl.interceptors;

import com.example.ecommerce.il.interfaces.CartService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
public class CartCountInterceptor implements HandlerInterceptor {
    private final CartService cartService;
    public CartCountInterceptor(CartService cartService) { this.cartService = cartService; }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) {
        if (modelAndView != null) {
            int count = cartService.getItemCount(request.getSession());
            modelAndView.addObject("cartItemCount", count);
        }
    }
}