/*
 * Copyright (c) 2015-2021 VMware Inc. or its affiliates, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

$(function(){
    // Main JS
    var navVisible = false;
    var toggle_el = $("[data-toggle]").data('toggle');

    $("[data-toggle]").on("click", function() {
        $(toggle_el).toggleClass("open-sidebar");
        navVisible = !navVisible;
        return false;
    });

    $("#content").on("click", function(){
        if (navVisible) {
            navVisible = false;
            $(toggle_el).removeClass("open-sidebar");
        }
    });
    $(window).on("resize", function(){
        if ($(this).width() > 800 && navVisible) {
            navVisible = false;
            $(toggle_el).removeClass("open-sidebar");
        }
    });

    var uls = $("#nav").html();
    $("#sidebar").html(uls);

});