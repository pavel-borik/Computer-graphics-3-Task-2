#version 330
in vec2 inPosition;
out vec3 viewDirection, lightDirection, normal;
out float dist;
out vec4 position, shadowCoord;
uniform int object, lightMode;
uniform mat4 modelMat, viewMat, projMat, lightMVP;
uniform vec3 baseCol, lightDir;
const float PI = 3.1415926535897932384626433832795;

vec3 createObject(vec2 uv);
vec3 normalDiff (vec2 uv);

void main() {
    // Calculated in World space
    // Using the directional light only
    position = vec4(createObject(inPosition), 1.0);
    normal = normalDiff(inPosition);
    normal = transpose(inverse(mat3(modelMat))) * normal;
	gl_Position = projMat * viewMat * modelMat * position;

    vec4 objectPosition =  modelMat * position;
    lightDirection = lightDir;
    viewDirection = -objectPosition.xyz;
    dist = length(lightDirection);

    //Matrix used to modify the coords from <-1;1> to <0;1>
    mat4 biasMatrix= mat4(
    0.5, 0.0, 0.0, 0.0,
    0.0, 0.5, 0.0, 0.0,
    0.0, 0.0, 0.5, 0.0,
    0.5, 0.5, 0.5, 1.0
    );

    mat4 depthBiasMVP = biasMatrix * lightMVP;

    shadowCoord = depthBiasMVP * position;
}

vec3 createObject (vec2 uv) {
    float r,s,t,x,y,z;
    switch(object) {
        //Floor
        case 0:
           return vec3(50*(uv.x-0.5), 50*(uv.y-0.5), 0);
        //Torus
        case 1:
           s = 2* PI * uv.x;
           t = 2* PI * uv.y;
           return vec3(3*cos(s)+cos(t)*cos(s) + 15, 3*sin(s)+cos(t)*sin(s), sin(t)+5);
        //Mushroom
        case 2:
           s = 2* PI * uv.x;
           t = 2* PI * uv.y;
           r = (1+max(sin(t),0))*2;
         return vec3(r * sin(s), r * cos(s), (3-t)+3.2);
        //Sphere
        case 3:
           s = PI * 0.5 - PI*uv.y;
           t = PI * 2 * uv.x;
           r = 3;
           return vec3(r * cos(s)*sin(t) + 8, r * cos(s)*cos(t), r * sin(s) + 10);

    }
    return vec3(0,0,0);
}

vec3 normalDiff (vec2 uv){
    float delta = 0.0001;
    vec3 dzdu = (createObject(uv+vec2(delta,0))-createObject(uv-vec2(delta,0)))/delta;
    vec3 dzdv = (createObject(uv+vec2(0,delta))-createObject(uv-vec2(0,delta)))/delta;
    return cross(dzdu,dzdv);
}


