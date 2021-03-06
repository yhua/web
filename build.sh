#!/bin/sh

#npm install yo
#npm install coffee-script
#npm install coffee-script-redux
#npm install karma
#npm install generator-angular
#npm install phantomjs
#sudo npm install -g PhantomJS
#npm install karma-phantomjs-launcher --save-dev

function load_env {
  [ -f ./kulebao_config/dev_env.sh ] && source ./kulebao_config/dev_env.sh
}

function build_local {
    load_env
    JAVA_OPTS=-Xmx2048m karma start --single-run && \
    play pmd checkstyle findbugs test
}

function build_and_push {
    git submodule update &&
    git pull --rebase && \
    build_local && \
    git push origin master
}

function deploy {
    build_local && \
    git push heroku master
}

function all {
    build_local && \
    git push origin master && \
    git push heroku master
}

function local_https_server {
    load_env
    JAVA_OPTS=-Dhttps.port=9001 play run
}

function deploy_prod {
    now=$(date +"%s")
    srcFilename="$(pwd)/target/universal/kulebao-1.0-SNAPSHOT.zip"
    destFilename="kulebao-1.0-SNAPSHOT.$now.zip"
    destServer="kulebao@115.28.7.205"
    destPath="$destServer:~/$destFilename"
    play dist && \
    scp $srcFilename $destPath && \
    ssh $destServer "unzip -x $destFilename -d /var/play/$now/" && \
    ssh $destServer "rm /var/play/kulebao" && \
    ssh $destServer "ln -s /var/play/$now/kulebao-1.0-SNAPSHOT/ /var/play/kulebao" && \
    ssh $destServer "echo coco999 | sudo -S service kulebao restart"

    retvalue=$?
    echo "Return value: $retvalue"
    echo "Done"
}


function deploy_from_prod {
    now=$(date +"%s")
    srcFilename="$(pwd)/target/universal/kulebao-1.0-SNAPSHOT.zip"
    destFilename="kulebao-1.0-SNAPSHOT.$now.zip"
    play dist && \
    cp $srcFilename ~/$destFilename
    unzip -x $destFilename -d /var/play/$now/ && \
    rm /var/play/kulebao && \
    ln -s /var/play/$now/kulebao-1.0-SNAPSHOT/ /var/play/kulebao && \
    echo coco999 | sudo -S service kulebao restart  && \
    echo coco999 | sudo -S /usr/sbin/nginx -s reload

    retvalue=$?
    echo "Return value: $retvalue"
    echo "Done"
}

function js_dependency {
  grunt
}

function main {
  	case $1 in
		js) js_dependency ;;
		s) local_https_server ;;
		a) all ;;
		d) deploy ;;
		d2) deploy_from_prod ;;
		prod) deploy_prod ;;
		p) build_and_push ;;
		b) build_local ;;
		*) build_local ;;
	esac
}

main $@
