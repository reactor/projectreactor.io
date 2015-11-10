/*
 * Copyright (c) 2011-2015 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var gulp = require('gulp'),
    gutil = require('gulp-util'),
    rename = require('gulp-rename'),
    compass = require('gulp-compass'),
    cssmin = require('gulp-cssmin'),
    concat = require('gulp-concat'),
    del = require('del');

var root = './src/main/static/';
var dist = './src/main/resources/public/';

module.exports = function(config){
    return function(){
        var pipe = gulp.src([root+'assets/css/app.scss', root+'assets/css/docs.scss'])
            .pipe(compass({
                sass: root+'assets/css',
                image: root+'assets/img',
                css: dist+'assets/css'
            }))
            .on('error', function(err){
                gutil.log('Error in SASS build:');
                gutil.log(err);
            })
            .pipe(gulp.dest(dist+'assets/css'));

        if(config.cssmin)
            pipe.pipe(cssmin());

        return pipe;
    };
};