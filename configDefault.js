module.exports = {
    build: {
        vendor: {
            js: [
                //'./public/src/js/code.js',
                './node_modules/es5-shim/es5-shim.js',
                './node_modules/jquery/dist/jquery.js'
                // './node_modules/underscore/underscore.min.js',
            ],
        },
        browserify: {
            paths: [
                './public/src',
                './node_modules'
            ],
            debug: true
        },
        clean: {
            copy: ['./src/main/resources/public/assets', './src/main/resources/public/**.html'],
            scripts: ['./src/main/resources/public/js/**.*'],
            styles: ['./src/main/resources/public/css/**.*'],
            fonts: ['./src/main/resources/public/fonts/**.*']
        },
        cssmin: false,
        uglify: false
    }
};