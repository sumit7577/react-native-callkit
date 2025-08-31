package com.callkit.callManager

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.transform.CircleCropTransformation
import com.callkit.R
import com.callkit.extensions.getCallDuration
import com.callkit.extensions.getStateCompat
import com.callkit.helpers.getCallContact
import com.callkit.models.CallContact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class ChatBubbleArrowShape(private val cornerSize: Dp) : Shape {
  override fun createOutline(
    size: Size,
    layoutDirection: LayoutDirection,
    density: Density
  ): Outline {
    // Convert Dp to Px
    val cornerSizePx = with(density) { cornerSize.toPx() }

    // Define the custom path for a circle with an arrow pointing up
    val path = Path().apply {
      // Draw the arrow pointing up
      moveTo(size.width / 2 - cornerSizePx, 0f) // Left side of the arrow
      lineTo(size.width / 2f, -cornerSizePx)    // Tip of the arrow (upwards)
      lineTo(size.width / 2 + cornerSizePx, 0f) // Right side of the arrow

      // Draw the circular shape
      arcTo(
        rect = androidx.compose.ui.geometry.Rect(
          0f, 0f, size.width, size.height
        ),
        startAngleDegrees = 0f,
        sweepAngleDegrees = 360f,
        forceMoveTo = false
      )

      close()
    }

    return Outline.Generic(path)
  }
}

data class Icons(
  val name:Int,
  val onClick:()->Unit,
  val background:Color,
  val foreground:Color,
  val isEnabled: Boolean = true
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DialpadContent(
  onDismiss: () -> Unit,
  onClick: (code:Char) -> Unit,
  dialPadCode: SnapshotStateList<Char>

) {
  val screenWidth = LocalConfiguration.current.screenWidthDp
  val size= screenWidth.dp /5
  val buttons = listOf(
    '1' to "",
  '2' to "ABC",
  '3' to "DEF",
  '4' to "GHI",
  '5' to "JKL",
  '6' to "MNO",
  '7' to "PQRS",
  '8' to "TUV",
  '9' to "WXYZ",
  '*' to "",
  '0' to "+",
  '#' to "")

  Column(
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ){

    Column(
      modifier = Modifier.fillMaxWidth()
        .padding(8.dp,0.dp)
    ) {
      BasicTextField(
        value = dialPadCode.joinToString(""),
        onValueChange = {},
        readOnly = true,
        maxLines = 1,
        modifier = Modifier.fillMaxWidth()
          .height(40.dp),
        textStyle = TextStyle(
          fontSize = 30.sp,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground,
        ),
        singleLine = true,
        visualTransformation = VisualTransformation.None,
      )
    }

    FlowRow(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      maxItemsInEachRow = 3
    ) {
      buttons.forEach { (number, letters) ->
        Box(
          modifier = Modifier
            .size(width = size *1.5f, height = size)
            .padding(4.dp)
            .clip(RoundedCornerShape(49))
            .background(Color.Gray)
            .clickable {
              onClick(number)
            },
          contentAlignment = Alignment.Center // Center text inside the box
        ) {
          Column(modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = number.toString(),
              fontSize = 30.sp,
              fontFamily = FontFamily.Default,
              textAlign = TextAlign.Center,
              fontWeight = FontWeight.Bold,
              color = Color.White
            )
            if (letters.isNotEmpty()) {
              Text(
                text = letters,
                fontSize = 12.sp,
                fontFamily = FontFamily.Default,
                textAlign = TextAlign.Center,
                color = Color.White.copy(alpha = 0.7f)
              )
            }
          }

        }
      }
    }
  }

}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnGoingCallScreen(
    modifier:Modifier = Modifier,
    onReject: ()-> Unit,
    callerNumber: String,
    state:Int,
    isSpeakerOn:Boolean,
    isMuted:Boolean,
    isHold:Boolean,
    toggleSpeaker:()->Unit,
    toggleMute:()->Unit,
    onAddCall:()->Unit,
    callState: PhoneState,
    showHoldUI: Boolean = false,
    activeCall: Call,
    holdCall:Call?= null,
    toggleHold:()->Unit,
    toggleMerge:()->Unit,
    toggleSwap:()->Unit,
    callAudioState: CallAudioState?
) {
  val coroutineScope = rememberCoroutineScope()
  var showDialpad by remember { mutableStateOf(false) }
  var showMore by remember { mutableStateOf(false) }
  var currCallInfo by remember { mutableStateOf<CallContact?>(null) }
  var isBluetoothAvailable by remember { mutableStateOf(false) }
  var isConference by remember { mutableStateOf(false) }
  var isOnHold by remember { mutableStateOf(showHoldUI) }
  var callDuration by remember { mutableStateOf(0L) }
  //Log.d("onGOinf",currCallInfo.toString())
  val context = LocalContext.current

  val dialPadCode = remember { mutableStateListOf<Char>() }

  LaunchedEffect(activeCall){
    if(activeCall.getStateCompat() == Call.STATE_ACTIVE && holdCall.getStateCompat() == Call.STATE_ACTIVE){
      currCallInfo = CallContact("Conference Call", "", "", "")
    }
    else{
      getCallContact(context,activeCall,callerNumber,{currCallInfo = it})
    }
  }

  LaunchedEffect(Unit){
    while(true){
      callDuration = activeCall.getCallDuration().toLong()
      delay(1000)
    }
  }

  LaunchedEffect(activeCall.getStateCompat(),holdCall.getStateCompat()){
    if(activeCall.getStateCompat() == Call.STATE_ACTIVE && holdCall.getStateCompat() == Call.STATE_ACTIVE){
      isConference = true
      isOnHold = false
    }
  }


  fun toggleShowMore(){
    if(showDialpad){
      showDialpad = !showDialpad
    }
    showMore = !showMore
  }

  fun toggleDialpad(){
    if(showMore){
      showMore = !showMore
    }
    showDialpad = !showDialpad
  }

  fun getColor(value:Boolean):Color
  {
    if(value){
      return Color.Black
    }
    return Color.White

  }
  LaunchedEffect(callAudioState){
    isBluetoothAvailable = callAudioState?.supportedBluetoothDevices?.isNotEmpty() == true
  }

  val icons = listOf(
    Icons(R.drawable.baseline_dialpad_24, onClick = {toggleDialpad()}, background = getColor(showDialpad), foreground = getColor(!showDialpad)),
    Icons(R.drawable.baseline_mic_off_24, onClick = toggleMute, background = getColor(isMuted), foreground = getColor(!isMuted)),
    Icons(if (isBluetoothAvailable) R.drawable.baseline_bluetooth_audio_24 else R.drawable.baseline_volume_up_24, onClick = toggleSpeaker, background = getColor(isSpeakerOn), foreground = getColor(!isSpeakerOn)),
    Icons(R.drawable.baseline_more_vert_24, onClick = { toggleShowMore() },background = getColor(showMore), foreground = getColor(!showMore))
  )

  val moreIcons = listOf(
    Icons(R.drawable.baseline_pause_24, onClick = { toggleHold() }, background = getColor(isHold), foreground = getColor(!isHold),
      isEnabled = activeCall.getStateCompat() == Call.STATE_ACTIVE || activeCall.getStateCompat() == Call.STATE_HOLDING),
    Icons(R.drawable.baseline_add_call_24, onClick = { onAddCall() }, background = Color.White, foreground = Color.Black)
  )

  val moreIconsWithHold = listOf(
    Icons(R.drawable.baseline_swap_calls_24, onClick = { toggleSwap()}, background = Color.White, foreground = Color.Black,
      isEnabled = holdCall!== null && (holdCall.getStateCompat() == Call.STATE_ACTIVE || holdCall.getStateCompat() == Call.STATE_HOLDING) || !isConference
    ) ,
    Icons(R.drawable.baseline_add_call_24, onClick = {}, background = Color.White, foreground = Color.Black,
      isEnabled = holdCall == null
    ),
    Icons(R.drawable.baseline_call_merge_24, onClick ={ toggleMerge() }, background = Color.White, foreground = Color.Black,
      isEnabled = holdCall!= null && (holdCall.getStateCompat() == Call.STATE_ACTIVE || holdCall.getStateCompat() == Call.STATE_HOLDING) || !isConference
    )
  )

  val iconsToShow = if (holdCall != null) moreIconsWithHold else moreIcons


  val formattedTime = remember(callDuration) {
    val hours = callDuration / 3600
    val minutes = (callDuration % 3600) / 60
    val seconds = callDuration % 60
    if(hours > 0){
      String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    else{
      String.format("%02d:%02d", minutes, seconds)
    }
  }

  Column(modifier = modifier.fillMaxSize()
    .background(Color.White),
    verticalArrangement = Arrangement.SpaceBetween) {

    Box(modifier= modifier.fillMaxWidth()){

      Column(modifier = modifier
        .fillMaxWidth()
        .padding(0.dp, 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        Box(modifier = modifier
          .clip(CircleShape)
          .background(Color.Gray.copy(alpha = 0.2f))){
          val imageUri = currCallInfo?.photoUri.takeIf { !it.isNullOrEmpty() }
            ?: R.drawable.baseline_person_24

          AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
              .data(imageUri)
              .transformations(CircleCropTransformation())
              .build(),
            contentDescription = "Caller Avatar",
            modifier = Modifier.size(80.dp),
            placeholder = painterResource(R.drawable.baseline_person_24),
            error = painterResource(R.drawable.baseline_person_24)
          )
        }
        if(state == Call.STATE_DIALING || state  == Call.STATE_CONNECTING){
          Text(text=state.asString(),
            fontSize = 14.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium
          )
        }

        Text(
          text = currCallInfo?.name.takeIf { !it.isNullOrBlank() } ?: activeCall.details?.handle?.schemeSpecificPart.orEmpty(),
          fontSize = 30.sp,
          fontFamily = FontFamily.Default,
          fontWeight = FontWeight.Bold
        )

        if(state == Call.STATE_ACTIVE){
          Text(
            text = formattedTime,
            fontSize = 20.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            color = Color.Gray
          )
        }
      }
      if(isOnHold){
        Row(modifier = modifier.fillMaxWidth()
          .background(Color.Black.copy(alpha=0.8f))
          .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ){
          Image(painter = painterResource(R.drawable.baseline_add_call_24),
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.tint(Color.White)
          )

          Text(text=holdCall?.details?.handle?.schemeSpecificPart ?: "Unknown",
            fontSize = 18.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )

          Text(text=" - On hold",
            fontSize = 18.sp,
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )
        }
      }
    }

    Column(modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp, 12.dp, 0.dp, 0.dp))
      .background(Color.LightGray.copy(alpha = 0.4f))
      .padding(10.dp, 40.dp),
      verticalArrangement = Arrangement.spacedBy(40.dp)
    ){

      AnimatedVisibility(showDialpad) {
        DialpadContent(
          onDismiss = { showDialpad = !showDialpad },
          onClick = {
            dialPadCode.add(it)
            activeCall.playDtmfTone(it)
            CoroutineScope(Dispatchers.Main).launch {
              delay(150L)
              activeCall.stopDtmfTone()
            } },
          dialPadCode = dialPadCode
        )
      }

      AnimatedVisibility(showMore) {
        Row(
          modifier = modifier.fillMaxWidth()
          .padding(10.dp,0.dp),
          horizontalArrangement = Arrangement.spacedBy(50.dp, Alignment.End)
        ){
          iconsToShow.forEach{item ->
            Box(modifier = modifier
              .clip(CircleShape)
              .background(item.background)
              .padding(10.dp)
              .clickable(enabled = item.isEnabled) { item.onClick() }
            ){
              Image(painter = painterResource(item.name),
                contentDescription = null,
                modifier = Modifier.size(30.dp),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.tint(
                  if (item.isEnabled) item.foreground else item.foreground.copy(alpha = 0.3f)
                )
              )
            }
          }
        }
      }
      Row(modifier = modifier.fillMaxWidth()
        .padding(10.dp,0.dp),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        icons.forEach{item ->
          Box(modifier = modifier
            .clip(CircleShape)
            .background(item.background)
            .padding(10.dp)
            .clickable{ item.onClick() }
          ){
            Image(painter = painterResource(item.name),
              contentDescription = null,
              modifier = Modifier.size(30.dp),
              contentScale = ContentScale.Crop,
              colorFilter = ColorFilter.tint(item.foreground)
            )
          }
        }
      }

      Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
      ){
        Box(modifier = modifier
          .clip(CircleShape)
          .background(Color.Red)
          .padding(10.dp)
          .clickable { onReject() }
        ){
          Image(painter = painterResource(R.drawable.baseline_call_end_24),
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            colorFilter =  ColorFilter.tint(Color.White),
            contentScale = ContentScale.Crop)
        }
      }
    }

  }
}


@Preview(showBackground = true)
@Composable
fun OnGoingCallScreenPreview(modifier: Modifier = Modifier){
  //OnGoingCallScreen(onReject = {}, callerNumber = "+919117517898",state= Call.STATE_NEW, isMuted = true, isSpeakerOn = false, toggleMute = {}, toggleSpeaker = {}, onAddCall = {})

}
