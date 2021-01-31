function start(){
	var body=document.getElementsByTagName("BODY")[0];
	var box=document.getElementsByClassName("box1")[0];
	var icon=document.getElementsByTagName("img")[0];
	var rand=Math.floor(Math.random()*11+1);
	body.style.backgroundImage ="url(\"assets/background/"+rand+".jpg\")";
	if(rand==10){
		body.style.width=body.style.height="10000px";
		body.style.backgroundSize="auto";
	} 
	if(false||rand==2||rand==8){
		box.style.backgroundColor="rgba(0,0,0,0.4)";
		box.style.color="white";
		box.style.borderStyle="none";
	}
	if(false||rand==3||rand==7){
		box.style.backgroundColor="rgba(255,255,255,0.5)";
	}
	if(false||rand==4||rand==6){
		box.style.color="white";
		box.style.border="1px dashed white";
	}
}