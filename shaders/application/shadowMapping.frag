#version 330
in vec3 viewDirection, lightDirection, normal;
in float dist;
in vec4 position, shadowCoord;
uniform int object, aaMode;
uniform vec3 baseCol;
const float PI = 3.1415926535897932384626433832795;
out vec4 outColor;
uniform mat4 modelMat, viewMat, projMat;
uniform sampler2DShadow textureDepth;

// Random float number generator
float random(vec3 seed, int i){
	vec4 seed4 = vec4(seed,i);
	float dot_product = dot(seed4, vec4(12.9898,78.233,45.164,94.673));
	return fract(sin(dot_product) * 43758.5453);
}

void main( void ) {
    vec4 ambientLightCol = vec4(0.1,0.1, 0.1, 1.0);
    vec4 matSpecular = vec4(0.3, 0.3, 0.3, 1.0);
    vec4 lightCol = vec4(0.7, 0.7,0.7,1.0);
    vec4 matDiffuse = vec4(baseCol,1.0);
    float specularPower = 28;

    vec3 ld = normalize( lightDirection );
    vec3 nd = normalize( normal );
    vec3 vd = normalize( viewDirection );

    vec4 totalAmbient = ambientLightCol * matDiffuse;

    vec4 totalDiffuse = vec4(0.0);
    vec4 totalSpecular = vec4(0.0);
    float att;

    float NDotL = dot(nd, ld);
    if (NDotL > 0.0) {
        vec3 halfVector = normalize( ld + vd);
        float NDotH = max( 0.0, dot( nd, halfVector ) );
        totalDiffuse = lightCol * NDotL * matDiffuse;
        att = 1.0 / (1.0 + 0.01*dist + 0.001*dist*dist);
        totalSpecular = matSpecular * (pow(NDotH, specularPower));
    }
    //Pixel's visibility, gets lower if it's in the shadow
    float visibility = 1.0;
    //Bias value to prevent "shadow acne" artifacts
    float bias = 0.005;
    //Poisson disk used for anti-aliasing purposes
    vec2 poissonDisk[16] = vec2[](
      vec2( -0.94201624, -0.39906216 ),
      vec2( 0.94558609, -0.76890725 ),
      vec2( -0.094184101, -0.92938870 ),
      vec2( 0.34495938, 0.29387760 ),
      vec2( -0.91588581, 0.45771432 ),
      vec2( -0.81544232, -0.87912464 ),
      vec2( -0.38277543, 0.27676845 ),
      vec2( 0.97484398, 0.75648379 ),
      vec2( 0.44323325, -0.97511554 ),
      vec2( 0.53742981, -0.47373420 ),
      vec2( -0.26496911, -0.41893023 ),
      vec2( 0.79197514, 0.19090188 ),
      vec2( -0.24188840, 0.99706507 ),
      vec2( -0.81409955, 0.91437590 ),
      vec2( 0.19984126, 0.78641367 ),
      vec2( 0.14383161, -0.14100790 )
   );

    //Switching of anti-aliasing modes
    switch(aaMode) {
        // No anti-aliasing
        case 0:
            visibility -= 0.8* (1.0-texture( textureDepth, vec3(shadowCoord.xy,  (shadowCoord.z-bias)/shadowCoord.w) ));
            break;
        // Poisson sampling
        case 1:
            for (int i=0;i<4;i++){
                visibility -= 0.2* (1.0-texture( textureDepth, vec3(shadowCoord.xy + poissonDisk[i]/700.0,  (shadowCoord.z-bias)/shadowCoord.w) ));
            }
            break;
        // Stratified Poisson sampling
        case 2:
            for (int i=0;i<4;i++){
                int index = int(16.0*random(floor(position.xyz*1000.0), i))%16;
                visibility -= 0.2* (1.0-texture( textureDepth, vec3(shadowCoord.xy + poissonDisk[index]/700.0,  (shadowCoord.z-bias)/shadowCoord.w) ));
            }
            break;
    }
    //Specular part is zero if a pixel is in the shadow
    if(visibility < 1.0) totalSpecular = vec4(0,0,0,0);

    outColor = totalAmbient+att*(visibility*totalDiffuse + totalSpecular);

}



