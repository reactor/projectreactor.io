/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
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
var gulp = require('gulp');

var root = './src/main/static/';
var dist = './src/main/resources/public/';

module.exports = function (config) {
    return function (forTask) {
        return function(done) {
            if (forTask == "internal") {
                return gulp.src(
                    [
                        root+'*.html',
                        root+'docs/index.html',
                        root+'favicon.ico',
                        root+'assets/img/**.*',
                        root+'assets/fonts/**.*'
                    ],
                    {base: root}
                ).pipe(gulp.dest(dist));
            }
            else if (forTask == "awesome") {
                return gulp.src(['./node_modules/font-awesome/fonts/**.*'], {base: './node_modules/font-awesome/'})
                    .pipe(gulp.dest(dist+'assets/'));
            }
        }
    };
};